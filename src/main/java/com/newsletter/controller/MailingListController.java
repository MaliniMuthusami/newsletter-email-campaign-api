package com.newsletter.controller;

import com.newsletter.dto.request.MailingListRequest;
import com.newsletter.dto.response.ApiResponse;
import com.newsletter.service.MailingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mailing-lists")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mailing Lists", description = "Manage mailing lists and subscribers")
public class MailingListController {

    private final MailingListService mailingListService;

    public MailingListController(MailingListService mailingListService) {
        this.mailingListService = mailingListService;
    }

    @PostMapping
    @Operation(summary = "Create a new mailing list")
    public ResponseEntity<ApiResponse.MailingListSummary> create(
            @Valid @RequestBody MailingListRequest.Create request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mailingListService.createMailingList(request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "Get all mailing lists for the authenticated user")
    public ResponseEntity<List<ApiResponse.MailingListSummary>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mailingListService.getAllMailingLists(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mailing list details including all subscribers")
    public ResponseEntity<ApiResponse.MailingListDetail> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mailingListService.getMailingListById(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a mailing list")
    public ResponseEntity<ApiResponse.MessageResponse> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        mailingListService.deleteMailingList(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.MessageResponse.ok("Mailing list deleted successfully"));
    }

    @PostMapping("/{id}/subscribers")
    @Operation(summary = "Add a subscriber to the mailing list")
    public ResponseEntity<ApiResponse.SubscriberResponse> addSubscriber(
            @PathVariable String id,
            @Valid @RequestBody MailingListRequest.AddSubscriber request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mailingListService.addSubscriber(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}/subscribers/{subscriberId}")
    @Operation(summary = "Remove a subscriber from the mailing list")
    public ResponseEntity<ApiResponse.MessageResponse> removeSubscriber(
            @PathVariable String id,
            @PathVariable String subscriberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        mailingListService.removeSubscriber(id, subscriberId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.MessageResponse.ok("Subscriber removed successfully"));
    }
}
