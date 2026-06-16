package com.example.aidemo.web;

import com.example.aidemo.service.ChatService;
import com.example.aidemo.service.KnowledgeService;
import com.example.aidemo.web.dto.AddDocumentRequest;
import com.example.aidemo.web.dto.ChatRequest;
import com.example.aidemo.web.dto.ChatResponse;
import com.example.aidemo.web.dto.SearchResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ChatService chatService;
    private final KnowledgeService knowledgeService;

    public ApiController(ChatService chatService, KnowledgeService knowledgeService) {
        this.chatService = chatService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request.question(), request.sessionId());
    }

    @PostMapping("/knowledge/documents")
    public Map<String, String> addDocument(@Valid @RequestBody AddDocumentRequest request) {
        String content = request.title() == null || request.title().isBlank()
                ? request.content()
                : request.title() + "\n\n" + request.content();
        knowledgeService.addDocument(content);
        return Map.of("status", "ok", "message", "document added");
    }

    @GetMapping("/knowledge/search")
    public SearchResponse search(@RequestParam("q") String query) {
        return knowledgeService.search(query);
    }
}
