package com.newsletter.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "campaigns")
public class Campaign {

    @Id
    private String id;

    private String name;
    private String subject;
    private String content;
    private CampaignStatus status = CampaignStatus.DRAFT;

    private String mailingListId;
    private String mailingListName;
    private String ownerId;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Campaign() {}

    public Campaign(String name, String subject, String content, String ownerId) {
        this.name = name;
        this.subject = subject;
        this.content = content;
        this.ownerId = ownerId;
        this.status = CampaignStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }
    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }
    public String getSubject()                         { return subject; }
    public void setSubject(String subject)             { this.subject = subject; }
    public String getContent()                         { return content; }
    public void setContent(String content)             { this.content = content; }
    public CampaignStatus getStatus()                  { return status; }
    public void setStatus(CampaignStatus status)        { this.status = status; }
    public String getMailingListId()                   { return mailingListId; }
    public void setMailingListId(String mailingListId) { this.mailingListId = mailingListId; }
    public String getMailingListName()                 { return mailingListName; }
    public void setMailingListName(String n)           { this.mailingListName = n; }
    public String getOwnerId()                         { return ownerId; }
    public void setOwnerId(String ownerId)             { this.ownerId = ownerId; }
    public LocalDateTime getScheduledAt()              { return scheduledAt; }
    public void setScheduledAt(LocalDateTime t)        { this.scheduledAt = t; }
    public LocalDateTime getSentAt()                   { return sentAt; }
    public void setSentAt(LocalDateTime t)             { this.sentAt = t; }
    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
}
