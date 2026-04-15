package com.newsletter.scheduler;

import com.newsletter.service.CampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CampaignScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);

    private final CampaignService campaignService;

    public CampaignScheduler(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /**
     * Runs every 60 seconds. Finds all SCHEDULED campaigns whose scheduledAt
     * time has passed, logs a simulated email send per subscriber, then marks
     * the campaign SENT.
     */
    @Scheduled(fixedRate = 60_000)
    public void checkAndSendDueCampaigns() {
        log.debug("Scheduler: checking for due campaigns...");
        campaignService.processDueCampaigns();
    }
}
