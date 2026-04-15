package com.newsletter.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "mailing_lists")
public class MailingList {

    @Id
    private String id;

    private String name;
    private String description;
    private String ownerId;

    private List<Subscriber> subscribers = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MailingList() {}

    public MailingList(String name, String description, String ownerId) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }
    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }
    public String getDescription()                     { return description; }
    public void setDescription(String description)     { this.description = description; }
    public String getOwnerId()                         { return ownerId; }
    public void setOwnerId(String ownerId)             { this.ownerId = ownerId; }
    public List<Subscriber> getSubscribers()           { return subscribers; }
    public void setSubscribers(List<Subscriber> s)     { this.subscribers = s; }
    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }
    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
}
