package com.newsletter.service.impl;

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
import com.newsletter.service.CampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignServiceImpl implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignServiceImpl.class);

    private final CampaignRepository campaignRepository;
    private final MailingListRepository mailingListRepository;
    private final UserRepository userRepository;

    public CampaignServiceImpl(CampaignRepository campaignRepository,
                                MailingListRepository mailingListRepository,
                                UserRepository userRepository) {
        this.campaignRepository = campaignRepository;
        this.mailingListRepository = mailingListRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse.CampaignResponse createCampaign(CampaignRequest.Create request, String username) {
        User owner = getUser(username);

        Campaign campaign = new Campaign(request.getName(), request.getSubject(), request.getContent(), owner.getId());

        if (request.getMailingListId() != null) {
            MailingList list = getMailingList(request.getMailingListId(), owner.getId());
            campaign.setMailingListId(list.getId());
            campaign.setMailingListName(list.getName());
        }

        campaignRepository.save(campaign);
        log.info("Campaign '{}' created as DRAFT by '{}'", campaign.getName(), username);
        return toResponse(campaign);
    }

    @Override
    public ApiResponse.CampaignResponse updateCampaign(String id, CampaignRequest.Update request, String username) {
        User owner = getUser(username);
        Campaign campaign = getOwnedCampaign(id, owner.getId());

        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new BadRequestException("Cannot edit a campaign that has already been sent");
        }

        if (request.getName() != null)    campaign.setName(request.getName());
        if (request.getSubject() != null) campaign.setSubject(request.getSubject());
        if (request.getContent() != null) campaign.setContent(request.getContent());

        if (request.getMailingListId() != null) {
            MailingList list = getMailingList(request.getMailingListId(), owner.getId());
            campaign.setMailingListId(list.getId());
            campaign.setMailingListName(list.getName());
        }

        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);
        log.info("Campaign '{}' updated by '{}'", campaign.getName(), username);
        return toResponse(campaign);
    }

    @Override
    public ApiResponse.CampaignResponse scheduleCampaign(String id, CampaignRequest.Schedule request, String username) {
        User owner = getUser(username);
        Campaign campaign = getOwnedCampaign(id, owner.getId());

        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new BadRequestException("Cannot schedule a campaign that has already been sent");
        }
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Scheduled time must be in the future");
        }

        MailingList list = getMailingList(request.getMailingListId(), owner.getId());

        campaign.setMailingListId(list.getId());
        campaign.setMailingListName(list.getName());
        campaign.setScheduledAt(request.getScheduledAt());
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setUpdatedAt(LocalDateTime.now());

        campaignRepository.save(campaign);
        log.info("Campaign '{}' scheduled for {} by '{}'", campaign.getName(), request.getScheduledAt(), username);
        return toResponse(campaign);
    }

    @Override
    public ApiResponse.CampaignResponse getCampaignById(String id, String username) {
        User owner = getUser(username);
        return toResponse(getOwnedCampaign(id, owner.getId()));
    }

    @Override
    public Page<ApiResponse.CampaignResponse> getAllCampaigns(String username, CampaignStatus status,
                                                               int page, int size) {
        User owner = getUser(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Campaign> campaigns = (status != null)
                ? campaignRepository.findByOwnerIdAndStatus(owner.getId(), status, pageable)
                : campaignRepository.findByOwnerId(owner.getId(), pageable);

        return campaigns.map(this::toResponse);
    }

    @Override
    public void deleteCampaign(String id, String username) {
        User owner = getUser(username);
        Campaign campaign = getOwnedCampaign(id, owner.getId());
        if (campaign.getStatus() == CampaignStatus.SENT) {
            throw new BadRequestException("Cannot delete a sent campaign");
        }
        campaignRepository.delete(campaign);
        log.info("Campaign '{}' deleted by '{}'", campaign.getName(), username);
    }

    @Override
    public void processDueCampaigns() {
        List<Campaign> due = campaignRepository.findDueCampaigns(CampaignStatus.SCHEDULED, LocalDateTime.now());
        for (Campaign campaign : due) {
            try {
                sendCampaign(campaign);
            } catch (Exception e) {
                log.error("Failed to process campaign id={}: {}", campaign.getId(), e.getMessage());
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sendCampaign(Campaign campaign) {
        log.info("========== SENDING CAMPAIGN: '{}' (id={}) ==========", campaign.getName(), campaign.getId());

        if (campaign.getMailingListId() == null) {
            log.warn("Campaign '{}' has no mailing list — skipping", campaign.getName());
            return;
        }

        MailingList list = mailingListRepository.findById(campaign.getMailingListId()).orElse(null);
        if (list == null) {
            log.warn("Campaign '{}': mailing list id={} not found", campaign.getName(), campaign.getMailingListId());
            return;
        }

        List<Subscriber> subscribers = list.getSubscribers();
        if (subscribers.isEmpty()) {
            log.warn("Campaign '{}': mailing list '{}' has no subscribers", campaign.getName(), list.getName());
        }

        for (Subscriber s : subscribers) {
            log.info("[EMAIL SENT] To: {} <{}> | Subject: {} | Campaign: {}",
                    s.getName(), s.getEmail(), campaign.getSubject(), campaign.getName());
        }

        campaign.setStatus(CampaignStatus.SENT);
        campaign.setSentAt(LocalDateTime.now());
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        log.info("========== CAMPAIGN '{}' SENT to {} subscriber(s) ==========",
                campaign.getName(), subscribers.size());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Campaign getOwnedCampaign(String campaignId, String ownerId) {
        return campaignRepository.findByIdAndOwnerId(campaignId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private MailingList getMailingList(String listId, String ownerId) {
        return mailingListRepository.findByIdAndOwnerId(listId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Mailing list", listId));
    }

    private ApiResponse.CampaignResponse toResponse(Campaign c) {
        ApiResponse.CampaignResponse r = new ApiResponse.CampaignResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setSubject(c.getSubject());
        r.setContent(c.getContent());
        r.setStatus(c.getStatus());
        r.setMailingListId(c.getMailingListId());
        r.setMailingListName(c.getMailingListName());
        r.setScheduledAt(c.getScheduledAt());
        r.setSentAt(c.getSentAt());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        return r;
    }
}
