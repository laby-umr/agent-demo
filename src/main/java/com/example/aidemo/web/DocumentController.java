package com.example.aidemo.web;

import com.example.aidemo.document.ingest.DocumentIngestionService;
import com.example.aidemo.document.qa.DocumentQaService;
import com.example.aidemo.web.dto.DocumentAskRequest;
import com.example.aidemo.web.dto.DocumentQaResponse;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentQaService documentQaService;

    public DocumentController(DocumentIngestionService ingestionService, DocumentQaService documentQaService) {
        this.ingestionService = ingestionService;
        this.documentQaService = documentQaService;
    }

    @GetMapping("/status")
    public DocumentIngestionService.IngestStatus status() {
        return ingestionService.getStatus();
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentIngestionService.IngestStatus ingestUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "async", defaultValue = "false") boolean async)
            throws Exception {
        Path temp = Path.of(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
        file.transferTo(temp);
        if (async) {
            ingestionService.ingestAsync(temp);
            return ingestionService.getStatus();
        }
        return ingestionService.ingest(temp);
    }

    @PostMapping("/ingest-path")
    public DocumentIngestionService.IngestStatus ingestPath(
            @RequestParam("path") String path,
            @RequestParam(value = "async", defaultValue = "false") boolean async)
            throws Exception {
        Path pdfPath = Path.of(path);
        if (async) {
            ingestionService.ingestAsync(pdfPath);
            return ingestionService.getStatus();
        }
        return ingestionService.ingest(pdfPath);
    }

    @PostMapping("/ask")
    public DocumentQaResponse ask(@Valid @RequestBody DocumentAskRequest request) {
        return documentQaService.ask(request.question(), request.sessionId());
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("q") String query) {
        return Map.of("query", query, "hits", documentQaService.search(query));
    }
}
