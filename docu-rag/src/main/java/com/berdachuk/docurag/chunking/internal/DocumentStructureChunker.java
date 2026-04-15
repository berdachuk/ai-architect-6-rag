package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentStructureChunker implements Chunker {

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<ContentBlock> blocks = parseDocumentStructure(text);
        return buildChunksFromBlocks(blocks, chunkSize, overlap, minChars);
    }

    @Override
    public String getName() {
        return "DOCUMENT_STRUCTURE";
    }

    enum BlockType {
        PARAGRAPH,
        HEADING,
        LIST_ITEM
    }

    static class ContentBlock {
        final String content;
        final BlockType type;
        final int headingLevel;

        ContentBlock(String content, BlockType type) {
            this(content, type, 0);
        }

        ContentBlock(String content, BlockType type, int headingLevel) {
            this.content = content;
            this.type = type;
            this.headingLevel = headingLevel;
        }

        int length() {
            return content.length();
        }
    }

    static List<ContentBlock> parseDocumentStructure(String text) {
        List<ContentBlock> blocks = new ArrayList<>();

        String[] paragraphs = PARAGRAPH_PATTERN.split(text);

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(para);
            if (headingMatcher.find() && headingMatcher.start() == 0) {
                String headingText = para.substring(headingMatcher.start(), headingMatcher.end());
                int level = countHashes(headingMatcher.group());
                blocks.add(new ContentBlock(headingText, BlockType.HEADING, level));
                String rest = para.substring(headingMatcher.end()).trim();
                if (!rest.isEmpty()) {
                    blocks.add(new ContentBlock(rest, BlockType.PARAGRAPH));
                }
            } else if (para.startsWith("- ") || para.startsWith("* ") || para.matches("^\\d+\\.\\s+.+")) {
                blocks.add(new ContentBlock(trimmed, BlockType.LIST_ITEM));
            } else {
                blocks.add(new ContentBlock(trimmed, BlockType.PARAGRAPH));
            }
        }

        return blocks;
    }

    private static int countHashes(String heading) {
        int count = 0;
        for (char c : heading.toCharArray()) {
            if (c == '#') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    static List<String> buildChunksFromBlocks(List<ContentBlock> blocks, int chunkSize, int overlap, int minChars) {
        if (blocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (ContentBlock block : blocks) {
            String blockContent = block.content;
            int blockLen = blockContent.length();

            if (blockLen > chunkSize) {
                if (currentChunk.length() > 0) {
                    String chunk = currentChunk.toString().trim();
                    if (chunk.length() >= minChars) {
                        chunks.add(chunk);
                    }
                    currentChunk = new StringBuilder();
                    currentSize = 0;
                }

                List<String> subChunks = splitBlockRecursively(blockContent, chunkSize, overlap, minChars);
                chunks.addAll(subChunks.subList(0, subChunks.size() - 1));
                if (!subChunks.isEmpty()) {
                    currentChunk.append(subChunks.get(subChunks.size() - 1));
                    currentSize = currentChunk.length();
                }
            } else if (currentSize + blockLen + (currentSize > 0 ? 1 : 0) <= chunkSize) {
                if (currentSize > 0) {
                    currentChunk.append("\n\n");
                    currentSize += 2;
                }
                currentChunk.append(blockContent);
                currentSize += blockLen;
            } else {
                String chunk = currentChunk.toString().trim();
                if (chunk.length() >= minChars) {
                    chunks.add(chunk);
                }

                String overlapText = getOverlapText(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText);
                if (currentChunk.length() > 0 && blockContent.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(blockContent);
                currentSize = currentChunk.length();
            }
        }

        if (currentChunk.length() > 0) {
            String chunk = currentChunk.toString().trim();
            if (chunk.length() >= minChars) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private static String getOverlapText(String chunk, int overlapChars) {
        if (overlapChars <= 0 || chunk.isEmpty()) {
            return "";
        }
        int start = Math.max(0, chunk.length() - overlapChars);
        String overlap = chunk.substring(start);
        int paragraphBreak = overlap.indexOf("\n\n");
        if (paragraphBreak > 0) {
            return overlap.substring(paragraphBreak + 2);
        }
        return overlap;
    }

    private static List<String> splitBlockRecursively(String text, int chunkSize, int overlap, int minChars) {
        List<String> result = new ArrayList<>();
        String[] parts = text.split("\\n\\n");

        if (parts.length <= 1) {
            parts = text.split("\\n");
        }

        if (parts.length <= 1) {
            return charChunk(text, chunkSize, minChars);
        }

        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (current.length() + trimmed.length() + 2 <= chunkSize) {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(trimmed);
            } else {
                if (current.length() > 0) {
                    String chunk = current.toString().trim();
                    if (chunk.length() >= minChars) {
                        result.add(chunk);
                    }
                }
                current = new StringBuilder(trimmed);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        if (result.isEmpty()) {
            result.add(text);
        }

        return result;
    }

    private static List<String> charChunk(String text, int chunkSize, int minChars) {
        List<String> out = new ArrayList<>();
        int start = 0;
        int n = text.length();

        while (start < n) {
            int end = Math.min(n, start + chunkSize);
            String slice = text.substring(start, end);

            if (slice.length() >= minChars || (out.isEmpty() && slice.length() > 0)) {
                out.add(slice);
            } else if (!out.isEmpty() && !slice.isEmpty()) {
                String last = out.remove(out.size() - 1);
                out.add(last + slice);
            }

            if (end >= n) {
                break;
            }

            start = end;
        }

        return out;
    }
}