package com.newsletter.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CampaignRequest {

    public static class Create {

        @NotBlank(message = "Campaign name is required")
        @Size(max = 200)
        private String name;

        @NotBlank(message = "Subject is required")
        @Size(max = 300)
        private String subject;

        @NotBlank(message = "Content is required")
        private String content;

        private String mailingListId;

        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getSubject()                     { return subject; }
        public void setSubject(String subject)         { this.subject = subject; }
        public String getContent()                     { return content; }
        public void setContent(String content)         { this.content = content; }
        public String getMailingListId()               { return mailingListId; }
        public void setMailingListId(String id)        { this.mailingListId = id; }
    }

    public static class Update {

        @Size(max = 200)
        private String name;

        @Size(max = 300)
        private String subject;

        private String content;
        private String mailingListId;

        public String getName()                        { return name; }
        public void setName(String name)               { this.name = name; }
        public String getSubject()                     { return subject; }
        public void setSubject(String subject)         { this.subject = subject; }
        public String getContent()                     { return content; }
        public void setContent(String content)         { this.content = content; }
        public String getMailingListId()               { return mailingListId; }
        public void setMailingListId(String id)        { this.mailingListId = id; }
    }

    public static class Schedule {

        @NotNull(message = "Scheduled time is required")
        @Future(message = "Scheduled time must be in the future")
        private LocalDateTime scheduledAt;

        @NotNull(message = "Mailing list ID is required")
        private String mailingListId;

        public LocalDateTime getScheduledAt()          { return scheduledAt; }
        public void setScheduledAt(LocalDateTime t)    { this.scheduledAt = t; }
        public String getMailingListId()               { return mailingListId; }
        public void setMailingListId(String id)        { this.mailingListId = id; }
    }
}
