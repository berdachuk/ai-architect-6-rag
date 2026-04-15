package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
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

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DemoPagesController {

    private final IndexOperationsApi indexOperationsApi;
    private final DocumentCatalogApi documentCatalogApi;
    private final DocumentIngestApi documentIngestApi;
    private final DocumentIngestOrchestrator ingestOrchestrator;
    private final IndexingProgressApi indexingProgressApi;
    private final RagAskApi ragAskApi;
    private final EvaluationApi evaluationApi;

    @GetMapping("/")
    public String home(Model model) {
        IndexStatus status = indexOperationsApi.getStatus();
        model.addAttribute("indexStatus", status);
        model.addAttribute("docCount", status.documentCount());
        Optional<EvaluationRunDetail> latest = evaluationApi.getLatestRun();
        model.addAttribute("latestEval", latest.orElse(null));
        return "home";
    }

    @GetMapping("/qa")
    public String qaForm(Model model) {
        model.addAttribute("ragResult", null);
        return "qa";
    }

    @PostMapping("/qa")
    public String qaSubmit(
            Model model,
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double minScore
    ) {
        RagAskResponse rag = ragAskApi.ask(new RagAskRequest(question, topK, minScore));
        model.addAttribute("ragResult", rag);
        model.addAttribute("question", question);
        model.addAttribute("topK", topK);
        model.addAttribute("minScore", minScore);
        return "qa";
    }

    @GetMapping("/documents")
    public String documents(Model model, @RequestParam(defaultValue = "0") int page) {
        populateDocumentsPage(model, page);
        return "documents";
    }

    @PostMapping("/documents/ingest")
    public String ingestConfigured(Model model) {
        String runId = ingestOrchestrator.startConfiguredIngestAndIndex();
        model.addAttribute("indexActionMessage", "Started ingest + indexing run " + runId + ". Progress is updating below.");
        populateDocumentsPage(model, 0);
        return "documents";
    }

    @PostMapping("/documents/ingest-path")
    public String ingestCustomPath(
            Model model,
            @RequestParam String ingestPath
    ) {
        String selectedPath = ingestPath == null ? "" : ingestPath.trim();
        model.addAttribute("selectedIngestPath", selectedPath);
        if (selectedPath.isBlank()) {
            model.addAttribute("indexActionMessage", "Please provide a folder or file path to ingest.");
            populateDocumentsPage(model, 0);
            return "documents";
        }
        String runId = ingestOrchestrator.startPathIngestAndIndex(List.of(selectedPath));
        model.addAttribute("indexActionMessage", "Started ingest + indexing run " + runId + ". Progress is updating below.");
        populateDocumentsPage(model, 0);
        return "documents";
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
        } catch (Exception e) {
            model.addAttribute("indexActionMessage", "Folder ingest failed: " + e.getMessage());
        }

        populateDocumentsPage(model, 0);
        return "documents";
    }

    @PostMapping("/documents/index/clear-embeddings")
    public String clearEmbeddings(Model model, @RequestParam(defaultValue = "0") int page) {
        int affected = indexOperationsApi.clearEmbeddings();
        model.addAttribute("indexActionMessage", "Cleared embeddings for " + affected + " chunk(s).");
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

    @GetMapping("/analysis")
    public String analysis(Model model) {
        return "analysis";
    }

    @GetMapping("/documents/progress")
    @ResponseBody
    public ResponseEntity<ProgressSnapshot> documentsProgress() {
        return ResponseEntity.ok(indexingProgressApi.snapshot());
    }

    @PostMapping("/documents/select-folder")
    @ResponseBody
    public ResponseEntity<FolderSelectionResponse> selectFolder(
            @RequestParam(required = false) String currentPath
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            return ResponseEntity.status(409).body(new FolderSelectionResponse(
                    null,
                    true,
                    "Native folder dialog is not available in headless mode."
            ));
        }
        try {
            Optional<String> selected = chooseFolder(currentPath);
            if (selected.isPresent()) {
                return ResponseEntity.ok(new FolderSelectionResponse(selected.get(), false, null));
            }
            return ResponseEntity.ok(new FolderSelectionResponse(null, true, null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new FolderSelectionResponse(
                    null,
                    true,
                    "Failed to open folder chooser: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/evaluation")
    public String evaluationForm(Model model) {
        model.addAttribute("runs", evaluationApi.listRuns());
        model.addAttribute("runResult", null);
        model.addAttribute("runError", null);
        model.addAttribute("highlightRunId", null);
        return "evaluation";
    }

    @PostMapping("/evaluation/run")
    public String evaluationRun(
            Model model,
            @RequestParam String datasetName,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double minScore,
            @RequestParam(defaultValue = "0.8") double semanticPassThreshold
    ) {
        model.addAttribute("runError", null);
        try {
            var started = evaluationApi.run(new EvaluationRunRequest(datasetName, topK, minScore, semanticPassThreshold));
            model.addAttribute("runResult", started);
            model.addAttribute("highlightRunId", started.runId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("runResult", null);
            model.addAttribute("runError", ex.getMessage());
            model.addAttribute("highlightRunId", null);
        }
        model.addAttribute("runs", evaluationApi.listRuns());
        return "evaluation";
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
        List<Path> candidates = List.of(
                Path.of("data/corpus/_sample"),
                Path.of("data/pdf-demo/downloaded")
        );
        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.exists(absolute)) {
                return absolute.toString();
            }
        }
        return candidates.getFirst().toAbsolutePath().normalize().toString();
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
                        } catch (IOException ignored) {
                            // Best effort temp cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort temp cleanup.
        }
    }

    private Optional<String> chooseFolder(String currentPath) throws InvocationTargetException, InterruptedException {
        Path initialPath = null;
        if (currentPath != null && !currentPath.isBlank()) {
            try {
                Path candidate = Path.of(currentPath.trim()).toAbsolutePath().normalize();
                if (Files.isDirectory(candidate)) {
                    initialPath = candidate;
                }
            } catch (Exception ignored) {
                // Fall back to user home.
            }
        }
        File initialDirectory = initialPath != null
                ? initialPath.toFile()
                : Path.of(System.getProperty("user.home")).toFile();

        AtomicReference<File> selectedDir = new AtomicReference<>();
        Runnable chooseTask = () -> {
            JFileChooser chooser = new JFileChooser(initialDirectory);
            chooser.setDialogTitle("Choose data folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                selectedDir.set(chooser.getSelectedFile());
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            chooseTask.run();
        } else {
            SwingUtilities.invokeAndWait(chooseTask);
        }
        File selected = selectedDir.get();
        if (selected == null) {
            return Optional.empty();
        }
        return Optional.of(selected.toPath().toAbsolutePath().normalize().toString());
    }

    private record FolderSelectionResponse(
            String selectedPath,
            boolean cancelled,
            String message
    ) {
    }
}
