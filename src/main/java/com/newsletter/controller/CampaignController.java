package com.newsletter.controller;

import com.newsletter.model.CampaignStatus;
import com.newsletter.dto.request.CampaignRequest;
import com.newsletter.dto.response.ApiResponse;
import com.newsletter.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/campaigns")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Campaigns", description = "Create, edit, schedule and track email campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @Operation(summary = "Create a new campaign (saved as DRAFT)")
    public ResponseEntity<ApiResponse.CampaignResponse> create(
            @Valid @RequestBody CampaignRequest.Create request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createCampaign(request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List all campaigns with pagination and optional status filter")
    public ResponseEntity<Page<ApiResponse.CampaignResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Filter by status: DRAFT, SCHEDULED, SENT, CANCELLED")
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                campaignService.getAllCampaigns(userDetails.getUsername(), status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a campaign by ID")
    public ResponseEntity<ApiResponse.CampaignResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(campaignService.getCampaignById(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a campaign (only DRAFT or SCHEDULED campaigns can be edited)")
    public ResponseEntity<ApiResponse.CampaignResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CampaignRequest.Update request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(campaignService.updateCampaign(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule a campaign for a future date/time")
    public ResponseEntity<ApiResponse.CampaignResponse> schedule(
            @PathVariable String id,
            @Valid @RequestBody CampaignRequest.Schedule request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(campaignService.scheduleCampaign(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a campaign (only DRAFT or SCHEDULED campaigns can be deleted)")
    public ResponseEntity<ApiResponse.MessageResponse> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        campaignService.deleteCampaign(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.MessageResponse.ok("Campaign deleted successfully"));
    }
}
