package com.newsletter.service.impl;

import com.newsletter.model.MailingList;
import com.newsletter.model.Subscriber;
import com.newsletter.model.User;
import com.newsletter.dto.request.MailingListRequest;
import com.newsletter.dto.response.ApiResponse;
import com.newsletter.exception.DuplicateResourceException;
import com.newsletter.exception.ResourceNotFoundException;
import com.newsletter.repository.MailingListRepository;
import com.newsletter.repository.UserRepository;
import com.newsletter.service.MailingListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MailingListServiceImpl implements MailingListService {

    private static final Logger log = LoggerFactory.getLogger(MailingListServiceImpl.class);

    private final MailingListRepository mailingListRepository;
    private final UserRepository userRepository;

    public MailingListServiceImpl(MailingListRepository mailingListRepository,
                                  UserRepository userRepository) {
        this.mailingListRepository = mailingListRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse.MailingListSummary createMailingList(MailingListRequest.Create request, String username) {
        User owner = getUser(username);

        if (mailingListRepository.existsByNameAndOwnerId(request.getName(), owner.getId())) {
            throw new DuplicateResourceException("A mailing list named '" + request.getName() + "' already exists");
        }

        MailingList list = new MailingList(request.getName(), request.getDescription(), owner.getId());
        mailingListRepository.save(list);
        log.info("Mailing list '{}' created by '{}'", list.getName(), username);
        return toSummary(list);
    }

    @Override
    public List<ApiResponse.MailingListSummary> getAllMailingLists(String username) {
        User owner = getUser(username);
        return mailingListRepository.findByOwnerId(owner.getId())
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public ApiResponse.MailingListDetail getMailingListById(String id, String username) {
        User owner = getUser(username);
        MailingList list = mailingListRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mailing list", id));
        return toDetail(list);
    }

    @Override
    public void deleteMailingList(String id, String username) {
        User owner = getUser(username);
        MailingList list = mailingListRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mailing list", id));
        mailingListRepository.delete(list);
        log.info("Mailing list '{}' deleted by '{}'", list.getName(), username);
    }

    @Override
    public ApiResponse.SubscriberResponse addSubscriber(String mailingListId,
                                                         MailingListRequest.AddSubscriber request,
                                                         String username) {
        User owner = getUser(username);
        MailingList list = mailingListRepository.findByIdAndOwnerId(mailingListId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mailing list", mailingListId));

        boolean duplicate = list.getSubscribers().stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(request.getEmail()));
        if (duplicate) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already subscribed to this list");
        }

        Subscriber subscriber = new Subscriber(UUID.randomUUID().toString(), request.getName(), request.getEmail());
        list.getSubscribers().add(subscriber);
        list.setUpdatedAt(LocalDateTime.now());
        mailingListRepository.save(list);

        log.info("Subscriber '{}' added to list '{}' by '{}'", request.getEmail(), list.getName(), username);
        return toSubscriberResponse(subscriber);
    }

    @Override
    public void removeSubscriber(String mailingListId, String subscriberId, String username) {
        User owner = getUser(username);
        MailingList list = mailingListRepository.findByIdAndOwnerId(mailingListId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mailing list", mailingListId));

        Subscriber target = list.getSubscribers().stream()
                .filter(s -> s.getId().equals(subscriberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber", subscriberId));

        list.getSubscribers().remove(target);
        list.setUpdatedAt(LocalDateTime.now());
        mailingListRepository.save(list);
        log.info("Subscriber '{}' removed from list '{}' by '{}'", target.getEmail(), list.getName(), username);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private ApiResponse.MailingListSummary toSummary(MailingList list) {
        ApiResponse.MailingListSummary s = new ApiResponse.MailingListSummary();
        s.setId(list.getId());
        s.setName(list.getName());
        s.setDescription(list.getDescription());
        s.setSubscriberCount(list.getSubscribers().size());
        s.setCreatedAt(list.getCreatedAt());
        s.setUpdatedAt(list.getUpdatedAt());
        return s;
    }

    private ApiResponse.MailingListDetail toDetail(MailingList list) {
        ApiResponse.MailingListDetail d = new ApiResponse.MailingListDetail();
        d.setId(list.getId());
        d.setName(list.getName());
        d.setDescription(list.getDescription());
        d.setSubscriberCount(list.getSubscribers().size());
        d.setCreatedAt(list.getCreatedAt());
        d.setUpdatedAt(list.getUpdatedAt());
        d.setSubscribers(list.getSubscribers().stream()
                .map(this::toSubscriberResponse)
                .collect(Collectors.toList()));
        return d;
    }

    private ApiResponse.SubscriberResponse toSubscriberResponse(Subscriber s) {
        ApiResponse.SubscriberResponse r = new ApiResponse.SubscriberResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setEmail(s.getEmail());
        r.setSubscribedAt(s.getSubscribedAt());
        return r;
    }
}
