package com.bridgeflow.api.glossary.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.auth.application.CurrentUser;
import com.bridgeflow.api.glossary.api.GlossaryModels.GlossaryTermResponse;
import com.bridgeflow.api.glossary.api.GlossaryModels.SaveGlossaryTermRequest;
import com.bridgeflow.api.glossary.application.GlossaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/glossary")
@Tag(name = "Glossary", description = "Project-scoped Japanese–Vietnamese terminology")
@SecurityRequirement(name = "bearerAuth")
public class GlossaryController {

    private final GlossaryService glossaryService;

    public GlossaryController(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @GetMapping
    @Operation(operationId = "listGlossaryTerms", summary = "Search project glossary terms")
    public List<GlossaryTermResponse> list(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @RequestParam(defaultValue = "") String query
    ) {
        return glossaryService.list(user.id(), projectId, query);
    }

    @PostMapping
    @Operation(operationId = "createGlossaryTerm", summary = "Create a glossary term")
    @ApiResponse(responseCode = "201", description = "Glossary term created")
    public ResponseEntity<GlossaryTermResponse> create(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @Valid @RequestBody SaveGlossaryTermRequest request
    ) {
        var term = glossaryService.create(user.id(), projectId, request);
        return ResponseEntity.created(URI.create(
            "/api/v1/projects/" + projectId + "/glossary/" + term.id()
        )).body(term);
    }

    @PatchMapping("/{termId}")
    @Operation(operationId = "updateGlossaryTerm", summary = "Update a glossary term")
    public GlossaryTermResponse update(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @PathVariable UUID termId,
        @Valid @RequestBody SaveGlossaryTermRequest request
    ) {
        return glossaryService.update(user.id(), projectId, termId, request);
    }

    @DeleteMapping("/{termId}")
    @Operation(operationId = "deleteGlossaryTerm", summary = "Delete a glossary term")
    @ApiResponse(responseCode = "204", description = "Glossary term deleted")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @PathVariable UUID termId
    ) {
        glossaryService.delete(user.id(), projectId, termId);
        return ResponseEntity.noContent().build();
    }
}
