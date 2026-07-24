package com.englishlearningcopilot.backend.controller;

import com.englishlearningcopilot.backend.dto.PlaceholderResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPlaceholderController {

    /**
     * ANY /api/admin/question-types
     * ANY /api/admin/question-types/{id}
     * ANY /api/admin/question-banks
     * ANY /api/admin/question-banks/{id}
     * ANY /api/admin/vocabulary-entries
     * ANY /api/admin/vocabulary-entries/{id}
     * Return a placeholder response for reserved admin APIs(都是占位的，尚未开发完！)
     */
    @RequestMapping({
            "/question-types",
            "/question-types/{id}",
            "/question-banks",
            "/question-banks/{id}",
            "/vocabulary-entries",
            "/vocabulary-entries/{id}"
    })
    public ResponseEntity<PlaceholderResponse> placeholder(HttpServletRequest request) {
        String resource = extractResource(request.getRequestURI());
        String operation = request.getMethod().toUpperCase(Locale.ROOT);
        PlaceholderResponse response = new PlaceholderResponse(
                resource,
                operation,
                "Admin API is reserved and not implemented yet."
        );
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    private String extractResource(String path) {
        String prefix = "/api/admin/";
        if (!path.startsWith(prefix)) {
            return "unknown";
        }
        String remainder = path.substring(prefix.length());
        int slashIndex = remainder.indexOf('/');
        return slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
    }
}
