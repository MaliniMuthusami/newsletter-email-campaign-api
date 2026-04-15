package com.newsletter.dto.response;

import com.newsletter.model.CampaignStatus;

import java.time.LocalDateTime;
import java.util.List;

public class ApiResponse {

    // ── Auth ─────────────────────────────────────────────────────────────────

    public static class Auth {
        private String token;
        private String type;
        private String username;
        private String email;

        public Auth() {}
        public Auth(String token, String type, String username, String email) {
            this.token = token; this.type = type;
            this.username = username; this.email = email;
        }

        public String getToken()               { return token; }
        public void setToken(String token)     { this.token = token; }
        public String getType()                { return type; }
        public void setType(String type)       { this.type = type; }
        public String getUsername()            { return username; }
        public void setUsername(String u)      { this.username = u; }
        public String getEmail()               { return email; }
        public void setEmail(String e)         { this.email = e; }
    }

    // ── Mailing List ─────────────────────────────────────────────────────────

    public static class MailingListSummary {
        private String id;
        private String name;
        private String description;
        private int subscriberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public MailingListSummary() {}

        public String getId()                          { return id; }
        public void setId(String id)                   { this.id = id; }
        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getDescription()                 { return description; }
        public void setDescription(String d)           { this.description = d; }
        public int getSubscriberCount()                { return subscriberCount; }
        public void setSubscriberCount(int c)          { this.subscriberCount = c; }
        public LocalDateTime getCreatedAt()            { return createdAt; }
        public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()            { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
    }

    public static class MailingListDetail {
        private String id;
        private String name;
        private String description;
        private List<SubscriberResponse> subscribers;
        private int subscriberCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public MailingListDetail() {}

        public String getId()                               { return id; }
        public void setId(String id)                        { this.id = id; }
        public String getName()                             { return name; }
        public void setName(String name)                    { this.name = name; }
        public String getDescription()                      { return description; }
        public void setDescription(String d)                { this.description = d; }
        public List<SubscriberResponse> getSubscribers()    { return subscribers; }
        public void setSubscribers(List<SubscriberResponse> s) { this.subscribers = s; }
        public int getSubscriberCount()                     { return subscriberCount; }
        public void setSubscriberCount(int c)               { this.subscriberCount = c; }
        public LocalDateTime getCreatedAt()                 { return createdAt; }
        public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()                 { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)           { this.updatedAt = t; }
    }

    public static class SubscriberResponse {
        private String id;
        private String name;
        private String email;
        private LocalDateTime subscribedAt;

        public SubscriberResponse() {}

        public String getId()                          { return id; }
        public void setId(String id)                   { this.id = id; }
        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getEmail()                       { return email; }
        public void setEmail(String email)             { this.email = email; }
        public LocalDateTime getSubscribedAt()         { return subscribedAt; }
        public void setSubscribedAt(LocalDateTime t)   { this.subscribedAt = t; }
    }

    // ── Campaign ─────────────────────────────────────────────────────────────

    public static class CampaignResponse {
        private String id;
        private String name;
        private String subject;
        private String content;
        private CampaignStatus status;
        private String mailingListId;
        private String mailingListName;
        private LocalDateTime scheduledAt;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public CampaignResponse() {}

        public String getId()                          { return id; }
        public void setId(String id)                   { this.id = id; }
        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getSubject()                     { return subject; }
        public void setSubject(String subject)         { this.subject = subject; }
        public String getContent()                     { return content; }
        public void setContent(String content)         { this.content = content; }
        public CampaignStatus getStatus()              { return status; }
        public void setStatus(CampaignStatus status)   { this.status = status; }
        public String getMailingListId()               { return mailingListId; }
        public void setMailingListId(String id)        { this.mailingListId = id; }
        public String getMailingListName()             { return mailingListName; }
        public void setMailingListName(String n)       { this.mailingListName = n; }
        public LocalDateTime getScheduledAt()          { return scheduledAt; }
        public void setScheduledAt(LocalDateTime t)    { this.scheduledAt = t; }
        public LocalDateTime getSentAt()               { return sentAt; }
        public void setSentAt(LocalDateTime t)         { this.sentAt = t; }
        public LocalDateTime getCreatedAt()            { return createdAt; }
        public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }
        public LocalDateTime getUpdatedAt()            { return updatedAt; }
        public void setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
    }

    // ── Generic message ───────────────────────────────────────────────────────

    public static class MessageResponse {
        private String message;
        private boolean success;

        public MessageResponse() {}
        public MessageResponse(String message, boolean success) {
            this.message = message; this.success = success;
        }

        public static MessageResponse ok(String message)   { return new MessageResponse(message, true); }
        public static MessageResponse fail(String message) { return new MessageResponse(message, false); }

        public String getMessage()              { return message; }
        public void setMessage(String message)  { this.message = message; }
        public boolean isSuccess()              { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse() {}
        public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
            this.status = status; this.error = error;
            this.message = message; this.timestamp = timestamp;
        }

        public int getStatus()                         { return status; }
        public void setStatus(int status)              { this.status = status; }
        public String getError()                       { return error; }
        public void setError(String error)             { this.error = error; }
        public String getMessage()                     { return message; }
        public void setMessage(String message)         { this.message = message; }
        public LocalDateTime getTimestamp()            { return timestamp; }
        public void setTimestamp(LocalDateTime t)      { this.timestamp = t; }
    }
}
