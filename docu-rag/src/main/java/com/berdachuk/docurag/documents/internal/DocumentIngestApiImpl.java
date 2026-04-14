package com.berdachuk.docurag.documents.internal;

import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.IdGenerator;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.documents.api.IngestSummary;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DocumentIngestApiImpl implements DocumentIngestApi {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestApiImpl.class);
    private static final int MIN_TEXT_LEN = 20;

    private final DocuRagProperties properties;
    private final DocumentJdbcRepository documents;
    private final IngestionJobRepository jobs;
    private final StructuredFileParser structuredParser;
    private final PdfTextExtractor pdfTextExtractor;

    @Override
    @Transactional
    public IngestSummary ingestConfiguredPaths() {
        List<String> paths = new ArrayList<>();
        String corpus = properties.getIngestion().getCorpusPath();
        String pdf = properties.getIngestion().getPdfDemoPath();
        if (corpus != null && !corpus.isBlank()) {
            paths.add(corpus.trim());
        }
        if (pdf != null && !pdf.isBlank()) {
            paths.add(pdf.trim());
        }
        return ingestPaths(paths);
    }

    @Override
    @Transactional
    public IngestSummary ingestPaths(List<String> paths) {
        String jobId = IdGenerator.generateId();
        OffsetDateTime started = OffsetDateTime.now();
        jobs.insertStarted(jobId, started);
        int loaded = 0;
        int skipped = 0;
        try {
            for (String raw : paths) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                Path p = Path.of(raw.trim()).toAbsolutePath().normalize();
                if (!Files.exists(p)) {
                    log.warn("Ingest path does not exist: {}", p);
                    continue;
                }
                if (Files.isDirectory(p)) {
                    try (Stream<Path> stream = Files.list(p)) {
                        List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                        for (Path f : files) {
                            int[] r = ingestPath(f);
                            loaded += r[0];
                            skipped += r[1];
                        }
                    }
                } else {
                    int[] r = ingestPath(p);
                    loaded += r[0];
                    skipped += r[1];
                }
            }
            jobs.finishSuccess(jobId, loaded, OffsetDateTime.now());
            return new IngestSummary(jobId, loaded, skipped, "COMPLETED", null);
        } catch (Exception e) {
            log.error("Ingest failed", e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            jobs.finishFailure(jobId, msg, OffsetDateTime.now());
            return new IngestSummary(jobId, loaded, skipped, "FAILED", msg);
        }
    }

    /** @return int[]{loaded, skipped} */
    private int[] ingestPath(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int loaded = 0;
        int skipped = 0;
        if (name.endsWith(".pdf")) {
            if (ingestPdf(path)) {
                loaded++;
            } else {
                skipped++;
            }
            return new int[]{loaded, skipped};
        }
        List<StructuredFileParser.ParsedDocument> rows = structuredParser.parseFile(path);
        for (StructuredFileParser.ParsedDocument row : rows) {
            if (ingestStructuredRow(row, path, "structured")) {
                loaded++;
            } else {
                skipped++;
            }
        }
        return new int[]{loaded, skipped};
    }

    private boolean ingestPdf(Path path) throws IOException {
        String text = pdfTextExtractor.extractText(path).trim();
        if (text.length() < MIN_TEXT_LEN) {
            return false;
        }
        String hash = ContentHasher.sha256Hex(text);
        if (documents.existsByContentHash(hash)) {
            return false;
        }
        String externalId = "pdf:" + path.getFileName();
        String id = IdGenerator.generateId();
        try {
            documents.insert(new SourceDocumentEntity(
                    id,
                    externalId,
                    path.getFileName().toString(),
                    "pdf-demo",
                    path.getFileName().toString(),
                    path.toUri().toString(),
                    text,
                    hash,
                    "pdf"
            ));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private boolean ingestStructuredRow(StructuredFileParser.ParsedDocument row, Path sourcePath, String formatLabel) {
        String text = row.text() == null ? "" : row.text().trim();
        if (text.length() < MIN_TEXT_LEN) {
            return false;
        }
        String hash = ContentHasher.sha256Hex(text);
        if (documents.existsByContentHash(hash)) {
            return false;
        }
        String id = IdGenerator.generateId();
        String extId = row.externalId() == null ? id : row.externalId();
        try {
            documents.insert(new SourceDocumentEntity(
                    id,
                    extId,
                    row.title(),
                    row.category(),
                    row.sourceName(),
                    row.sourceUrl(),
                    text,
                    hash,
                    formatLabel
            ));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
