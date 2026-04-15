package com.newsletter.service;

import com.newsletter.dto.request.MailingListRequest;
import com.newsletter.dto.response.ApiResponse;

import java.util.List;

public interface MailingListService {
    ApiResponse.MailingListSummary createMailingList(MailingListRequest.Create request, String username);
    List<ApiResponse.MailingListSummary> getAllMailingLists(String username);
    ApiResponse.MailingListDetail getMailingListById(String id, String username);
    void deleteMailingList(String id, String username);
    ApiResponse.SubscriberResponse addSubscriber(String mailingListId, MailingListRequest.AddSubscriber request, String username);
    void removeSubscriber(String mailingListId, String subscriberId, String username);
}
