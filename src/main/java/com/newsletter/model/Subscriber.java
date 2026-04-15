package com.newsletter.model;

import java.time.LocalDateTime;

/**
 * Embedded document stored inside MailingList.subscribers list.
 * Uses a generated UUID as id so individual subscribers can be addressed.
 */
public class Subscriber {

    private String id;
    private String name;
    private String email;
    private LocalDateTime subscribedAt;

    public Subscriber() {}

    public Subscriber(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subscribedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId()                              { return id; }
    public void setId(String id)                       { this.id = id; }
    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }
    public String getEmail()                           { return email; }
    public void setEmail(String email)                 { this.email = email; }
    public LocalDateTime getSubscribedAt()             { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime t)       { this.subscribedAt = t; }
}
