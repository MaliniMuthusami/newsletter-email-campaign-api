package com.newsletter.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public static class Register {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getUsername()              { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail()                 { return email; }
        public void setEmail(String email)       { this.email = email; }
        public String getPassword()              { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Login {

        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public String getUsername()              { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword()              { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
