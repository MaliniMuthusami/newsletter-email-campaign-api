package com.newsletter.service;

import com.newsletter.model.CampaignStatus;
import com.newsletter.dto.request.CampaignRequest;
import com.newsletter.dto.response.ApiResponse;
import org.springframework.data.domain.Page;

public interface CampaignService {
    ApiResponse.CampaignResponse createCampaign(CampaignRequest.Create request, String username);
    ApiResponse.CampaignResponse updateCampaign(String id, CampaignRequest.Update request, String username);
    ApiResponse.CampaignResponse scheduleCampaign(String id, CampaignRequest.Schedule request, String username);
    ApiResponse.CampaignResponse getCampaignById(String id, String username);
    Page<ApiResponse.CampaignResponse> getAllCampaigns(String username, CampaignStatus status, int page, int size);
    void deleteCampaign(String id, String username);
    void processDueCampaigns();
}
