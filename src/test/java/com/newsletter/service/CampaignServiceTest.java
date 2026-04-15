package com.newsletter.service;

import com.newsletter.model.Campaign;
import com.newsletter.model.CampaignStatus;
import com.newsletter.model.MailingList;
import com.newsletter.model.Subscriber;
import com.newsletter.model.User;
import com.newsletter.dto.request.CampaignRequest;
import com.newsletter.dto.response.ApiResponse;
import com.newsletter.exception.BadRequestException;
import com.newsletter.exception.ResourceNotFoundException;
import com.newsletter.repository.CampaignRepository;
import com.newsletter.repository.MailingListRepository;
import com.newsletter.repository.UserRepository;
import com.newsletter.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock CampaignRepository campaignRepository;
    @Mock MailingListRepository mailingListRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CampaignServiceImpl campaignService;

    private User user;
    private MailingList mailingList;

    @BeforeEach
    void setup() {
        user = new User("john", "john@example.com", "hashed");
        user.setId("user-001");

        mailingList = new MailingList("My List", "A test list", "user-001");
        mailingList.setId("list-001");

        Subscriber s1 = new Subscriber(UUID.randomUUID().toString(), "Alice", "alice@example.com");
        Subscriber s2 = new Subscriber(UUID.randomUUID().toString(), "Bob",   "bob@example.com");
        mailingList.getSubscribers().add(s1);
        mailingList.getSubscribers().add(s2);
    }

    // ── createCampaign ────────────────────────────────────────────────────────

    @Test
    void createCampaign_success_asDraft() {
        CampaignRequest.Create req = new CampaignRequest.Create();
        req.setName("Launch");
        req.setSubject("We launched!");
        req.setContent("Big news.");
        req.setMailingListId("list-001");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(mailingListRepository.findByIdAndOwnerId("list-001", "user-001")).thenReturn(Optional.of(mailingList));

        Campaign saved = new Campaign("Launch", "We launched!", "Big news.", "user-001");
        saved.setId("camp-001");
        saved.setMailingListId("list-001");
        saved.setMailingListName("My List");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(campaignRepository.save(any())).thenReturn(saved);

        ApiResponse.CampaignResponse resp = campaignService.createCampaign(req, "john");

        assertThat(resp.getName()).isEqualTo("Launch");
        assertThat(resp.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(resp.getMailingListId()).isEqualTo("list-001");
    }

    @Test
    void createCampaign_withoutMailingList_success() {
        CampaignRequest.Create req = new CampaignRequest.Create();
        req.setName("Draft Only");
        req.setSubject("Subject");
        req.setContent("Content");
        // no mailingListId

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Campaign saved = new Campaign("Draft Only", "Subject", "Content", "user-001");
        saved.setId("camp-002");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(campaignRepository.save(any())).thenReturn(saved);

        ApiResponse.CampaignResponse resp = campaignService.createCampaign(req, "john");

        assertThat(resp.getMailingListId()).isNull();
        assertThat(resp.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        verify(mailingListRepository, never()).findByIdAndOwnerId(any(), any());
    }

    // ── updateCampaign ────────────────────────────────────────────────────────

    @Test
    void updateCampaign_success() {
        Campaign existing = new Campaign("Old Name", "Old Subject", "Old Content", "user-001");
        existing.setId("camp-001");
        existing.setStatus(CampaignStatus.DRAFT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CampaignRequest.Update req = new CampaignRequest.Update();
        req.setName("New Name");
        req.setSubject("New Subject");

        ApiResponse.CampaignResponse resp = campaignService.updateCampaign("camp-001", req, "john");

        assertThat(resp.getName()).isEqualTo("New Name");
        assertThat(resp.getSubject()).isEqualTo("New Subject");
        assertThat(resp.getContent()).isEqualTo("Old Content"); // unchanged
    }

    @Test
    void updateSentCampaign_throwsBadRequest() {
        Campaign sent = new Campaign("Sent", "Sub", "Content", "user-001");
        sent.setId("camp-001");
        sent.setStatus(CampaignStatus.SENT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> campaignService.updateCampaign("camp-001", new CampaignRequest.Update(), "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sent");
    }

    // ── scheduleCampaign ─────────────────────────────────────────────────────

    @Test
    void scheduleCampaign_success() {
        Campaign campaign = new Campaign("To Schedule", "Sub", "Content", "user-001");
        campaign.setId("camp-001");
        campaign.setStatus(CampaignStatus.DRAFT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(campaign));
        when(mailingListRepository.findByIdAndOwnerId("list-001", "user-001")).thenReturn(Optional.of(mailingList));
        when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDateTime future = LocalDateTime.now().plusDays(3);
        CampaignRequest.Schedule req = new CampaignRequest.Schedule();
        req.setScheduledAt(future);
        req.setMailingListId("list-001");

        ApiResponse.CampaignResponse resp = campaignService.scheduleCampaign("camp-001", req, "john");

        assertThat(resp.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
        assertThat(resp.getScheduledAt()).isEqualTo(future);
        assertThat(resp.getMailingListId()).isEqualTo("list-001");
    }

    @Test
    void scheduleCampaign_pastDate_throwsBadRequest() {
        Campaign campaign = new Campaign("Past Camp", "Sub", "Content", "user-001");
        campaign.setId("camp-001");
        campaign.setStatus(CampaignStatus.DRAFT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(campaign));

        CampaignRequest.Schedule req = new CampaignRequest.Schedule();
        req.setScheduledAt(LocalDateTime.now().minusHours(1));
        req.setMailingListId("list-001");

        assertThatThrownBy(() -> campaignService.scheduleCampaign("camp-001", req, "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void scheduleSentCampaign_throwsBadRequest() {
        Campaign sent = new Campaign("Sent Camp", "Sub", "Content", "user-001");
        sent.setId("camp-001");
        sent.setStatus(CampaignStatus.SENT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(sent));

        CampaignRequest.Schedule req = new CampaignRequest.Schedule();
        req.setScheduledAt(LocalDateTime.now().plusDays(1));
        req.setMailingListId("list-001");

        assertThatThrownBy(() -> campaignService.scheduleCampaign("camp-001", req, "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sent");
    }

    // ── getAllCampaigns ───────────────────────────────────────────────────────

    @Test
    void getAllCampaigns_withStatusFilter() {
        Campaign draft = new Campaign("Draft Camp", "Sub", "Content", "user-001");
        draft.setId("camp-001");
        draft.setStatus(CampaignStatus.DRAFT);
        draft.setCreatedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByOwnerIdAndStatus(eq("user-001"), eq(CampaignStatus.DRAFT), any()))
                .thenReturn(new PageImpl<>(List.of(draft)));

        Page<ApiResponse.CampaignResponse> result =
                campaignService.getAllCampaigns("john", CampaignStatus.DRAFT, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    void getAllCampaigns_noFilter_returnsAll() {
        Campaign c1 = new Campaign("A", "Sub", "C", "user-001");
        c1.setId("1"); c1.setCreatedAt(LocalDateTime.now()); c1.setUpdatedAt(LocalDateTime.now());
        Campaign c2 = new Campaign("B", "Sub", "C", "user-001");
        c2.setId("2"); c2.setCreatedAt(LocalDateTime.now()); c2.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByOwnerId(eq("user-001"), any()))
                .thenReturn(new PageImpl<>(List.of(c1, c2)));

        Page<ApiResponse.CampaignResponse> result =
                campaignService.getAllCampaigns("john", null, 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // ── getCampaignById ───────────────────────────────────────────────────────

    @Test
    void getCampaignById_notOwned_throwsNotFound() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-999", "user-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaignById("camp-999", "john"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteCampaign ────────────────────────────────────────────────────────

    @Test
    void deleteCampaign_success() {
        Campaign campaign = new Campaign("Bye", "Sub", "Content", "user-001");
        campaign.setId("camp-001");
        campaign.setStatus(CampaignStatus.DRAFT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(campaign));

        campaignService.deleteCampaign("camp-001", "john");

        verify(campaignRepository).delete(campaign);
    }

    @Test
    void deleteSentCampaign_throwsBadRequest() {
        Campaign sent = new Campaign("Sent", "Sub", "Content", "user-001");
        sent.setId("camp-001");
        sent.setStatus(CampaignStatus.SENT);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(campaignRepository.findByIdAndOwnerId("camp-001", "user-001")).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> campaignService.deleteCampaign("camp-001", "john"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sent");
    }

    // ── processDueCampaigns ───────────────────────────────────────────────────

    @Test
    void processDueCampaigns_marksAsSentAndLogsEmails() {
        Campaign campaign = new Campaign("Due Camp", "Subject", "Body", "user-001");
        campaign.setId("camp-001");
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        campaign.setMailingListId("list-001");

        when(campaignRepository.findDueCampaigns(eq(CampaignStatus.SCHEDULED), any()))
                .thenReturn(List.of(campaign));
        when(mailingListRepository.findById("list-001")).thenReturn(Optional.of(mailingList));
        when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        campaignService.processDueCampaigns();

        verify(campaignRepository).save(argThat(c ->
                c.getStatus() == CampaignStatus.SENT && c.getSentAt() != null
        ));
    }

    @Test
    void processDueCampaigns_noCampaignsDue_doesNothing() {
        when(campaignRepository.findDueCampaigns(any(), any())).thenReturn(List.of());

        campaignService.processDueCampaigns();

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void processDueCampaigns_noMailingList_skipsButDoesNotFail() {
        Campaign campaign = new Campaign("No List", "Sub", "Body", "user-001");
        campaign.setId("camp-001");
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setMailingListId("missing-list");

        when(campaignRepository.findDueCampaigns(any(), any())).thenReturn(List.of(campaign));
        when(mailingListRepository.findById("missing-list")).thenReturn(Optional.empty());

        // Should not throw
        assertThatNoException().isThrownBy(() -> campaignService.processDueCampaigns());
        verify(campaignRepository, never()).save(any());
    }
}
