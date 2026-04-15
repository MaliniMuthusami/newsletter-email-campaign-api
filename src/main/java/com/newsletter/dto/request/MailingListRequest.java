package com.newsletter.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MailingListRequest {

    public static class Create {

        @NotBlank(message = "Mailing list name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;

        public String getName()                    { return name; }
        public void setName(String name)           { this.name = name; }
        public String getDescription()             { return description; }
        public void setDescription(String desc)    { this.description = desc; }
    }

    public static class AddSubscriber {

        @NotBlank(message = "Subscriber name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150)
        private String email;

        public String getName()              { return name; }
        public void setName(String name)     { this.name = name; }
        public String getEmail()             { return email; }
        public void setEmail(String email)   { this.email = email; }
    }
}
