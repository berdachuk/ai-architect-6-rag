package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexingProgressApi;
import com.berdachuk.docurag.vector.api.IndexingProgressApi.ProgressSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Comparator;

@Controller
@RequiredArgsConstructor
public class DocumentsController {

    private final IndexOperationsApi indexOperationsApi;
    private final DocumentCatalogApi documentCatalogApi;
    private final DocumentIngestOrchestrator ingestOrchestrator;
    private final IndexingProgressApi indexingProgressApi;
    private final DocuRagProperties properties;

    @GetMapping("/documents")
    public String documents(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String selectedIngestPath
    ) {
        if (selectedIngestPath != null && !selectedIngestPath.isBlank()) {
            model.addAttribute("selectedIngestPath", selectedIngestPath.trim());
        }
        populateDocumentsPage(model, page);
        return "documents";
    }

    @PostMapping("/documents/ingest")
    public String ingestConfigured(RedirectAttributes redirectAttributes) {
        String runId = ingestOrchestrator.startConfiguredIngestAndIndex();
        redirectAttributes.addFlashAttribute("indexActionMessage", "Started ingest + indexing run " + runId + ". Progress is updating below.");
        return "redirect:/documents";
    }

    @PostMapping("/documents/ingest-path")
    public String ingestCustomPath(
            Model model,
            @RequestParam String ingestPath,
            RedirectAttributes redirectAttributes
    ) {
        String selectedPath = ingestPath == null ? "" : ingestPath.trim();
        model.addAttribute("selectedIngestPath", selectedPath);
        if (selectedPath.isBlank()) {
            model.addAttribute("indexActionMessage", "Please provide a folder or file path to ingest.");
            populateDocumentsPage(model, 0);
            return "documents";
        }
        String runId = ingestOrchestrator.startPathIngestAndIndex(List.of(selectedPath));
        redirectAttributes.addAttribute("selectedIngestPath", selectedPath);
        redirectAttributes.addFlashAttribute("indexActionMessage", "Started ingest + indexing run " + runId + ". Progress is updating below.");
        return "redirect:/documents";
    }

    @PostMapping("/documents/ingest-upload")
    public String ingestUploadedFolder(
            Model model,
            @RequestParam(required = false) String selectedIngestPath,
            @RequestParam("files") MultipartFile[] files
    ) {
        String selectedPath = selectedIngestPath == null || selectedIngestPath.isBlank()
                ? resolveDefaultIngestPath()
                : selectedIngestPath.trim();
        model.addAttribute("selectedIngestPath", selectedPath);
        if (files == null || files.length == 0) {
            model.addAttribute("indexActionMessage", "No files selected in folder dialog.");
            populateDocumentsPage(model, 0);
            return "documents";
        }

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("docurag-upload-");
            int stored = 0;
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String original = file.getOriginalFilename() == null ? ("upload-" + i) : file.getOriginalFilename();
                String safe = Path.of(original).getFileName().toString();
                Path target = tmpDir.resolve(i + "_" + safe);
                file.transferTo(target);
                stored++;
            }

            if (stored == 0) {
                model.addAttribute("indexActionMessage", "No readable files found in selected folder.");
                populateDocumentsPage(model, 0);
                return "documents";
            }
            Path cleanupDir = tmpDir;
            String runId = ingestOrchestrator.startUploadedPathIngestAndIndex(
                    List.of(tmpDir.toString()),
                    () -> deleteRecursivelyQuietly(cleanupDir)
            );
            model.addAttribute("indexActionMessage", "Started ingest + indexing run " + runId + " from uploaded files. Progress is updating above.");
            populateDocumentsPage(model, 0);
            return "documents";
        } catch (Exception e) {
            model.addAttribute("indexActionMessage", "Folder ingest failed: " + e.getMessage());
            populateDocumentsPage(model, 0);
            return "documents";
        }
    }

    @PostMapping("/documents/index/clear-embeddings")
    public String clearEmbeddings(Model model, @RequestParam(defaultValue = "0") int page) {
        int affected = indexOperationsApi.clearEmbeddings();
        model.addAttribute("indexActionMessage", "Embeddings cleared: deleted " + affected + " chunk embeddings.");
        populateDocumentsPage(model, page);
        return "documents";
    }

    @PostMapping("/documents/index/clear-chunks")
    public String clearChunks(Model model, @RequestParam(defaultValue = "0") int page) {
        int affected = indexOperationsApi.clearChunks();
        model.addAttribute("indexActionMessage", "Full cleanup complete: deleted " + affected + " source document(s) and all associated chunks.");
        populateDocumentsPage(model, page);
        return "documents";
    }

    @GetMapping("/documents/progress")
    @ResponseBody
    public ResponseEntity<ProgressSnapshot> documentsProgress() {
        return ResponseEntity.ok(indexingProgressApi.snapshot());
    }

    @PostMapping("/documents/stop")
    public ResponseEntity<Void> stopIngest() {
        indexingProgressApi.stop();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/documents/set-folder")
    public String setFolderPath(
            Model model,
            @RequestParam String selectedIngestPath
    ) {
        String selectedPath = selectedIngestPath == null ? "" : selectedIngestPath.trim();
        model.addAttribute("selectedIngestPath", selectedPath);
        populateDocumentsPage(model, 0);
        return "documents";
    }

    @PostMapping("/documents/select-folder")
    @ResponseBody
    public ResponseEntity<FolderSelectionResponse> selectFolder(
            @RequestParam(required = false) String currentPath
    ) {
        Path current = resolveBrowsePath(currentPath);
        return ResponseEntity.ok(new FolderSelectionResponse(
                null,
                false,
                null,
                current.toString(),
                parentPath(current),
                rootEntries(),
                childDirectoryEntries(current)
        ));
    }

    private void populateDocumentsPage(Model model, int page) {
        model.addAttribute("documents", documentCatalogApi.listDocuments(page, 25));
        model.addAttribute("total", documentCatalogApi.countDocuments());
        model.addAttribute("page", page);
        model.addAttribute("indexStatus", indexOperationsApi.getStatus());
        if (!model.containsAttribute("selectedIngestPath")) {
            model.addAttribute("selectedIngestPath", resolveDefaultIngestPath());
        }
    }

    private String resolveDefaultIngestPath() {
        String corpus = properties.getIngestion().getCorpusPath();
        if (corpus != null && !corpus.isBlank() && Files.exists(Path.of(corpus))) {
            return corpus;
        }
        String pdf = properties.getIngestion().getPdfDemoPath();
        if (pdf != null && !pdf.isBlank() && Files.exists(Path.of(pdf))) {
            return pdf;
        }
        return "/path/to/your/data";
    }

    private void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException ignored) {
                            // Best effort temp cleanup.
                        }
                    });
        } catch (java.io.IOException ignored) {
            // Best effort temp cleanup.
        }
    }

    private Path resolveBrowsePath(String currentPath) {
        if (currentPath != null && !currentPath.isBlank()) {
            try {
                Path candidate = Path.of(currentPath.trim()).toAbsolutePath().normalize();
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
                Path parent = candidate.getParent();
                if (parent != null && Files.isDirectory(parent)) {
                    return parent;
                }
            } catch (Exception ignored) {
                // Fall through to configured/default roots.
            }
        }

        String configured = resolveDefaultIngestPath();
        try {
            Path candidate = Path.of(configured).toAbsolutePath().normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            Path parent = candidate.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent;
            }
        } catch (Exception ignored) {
            // Fall through to user home.
        }

        return Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
    }

    private String parentPath(Path current) {
        Path parent = current.getParent();
        return parent == null ? null : parent.toAbsolutePath().normalize().toString();
    }

    private List<FolderEntry> rootEntries() {
        return java.util.Arrays.stream(File.listRoots())
                .map(file -> file.toPath().toAbsolutePath().normalize())
                .map(path -> new FolderEntry(path.toString(), path.toString()))
                .toList();
    }

    private List<FolderEntry> childDirectoryEntries(Path current) {
        try (var stream = Files.list(current)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(Files::isReadable)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .limit(300)
                    .map(path -> path.toAbsolutePath().normalize())
                    .map(path -> new FolderEntry(path.toString(), path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private record FolderSelectionResponse(
            String selectedPath,
            boolean cancelled,
            String message,
            String currentPath,
            String parentPath,
            List<FolderEntry> roots,
            List<FolderEntry> directories
    ) {
    }

    private record FolderEntry(
            String path,
            String name
    ) {
    }
}
