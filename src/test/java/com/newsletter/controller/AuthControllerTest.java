package com.newsletter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsletter.dto.request.AuthRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().drop();
    }

    @Test
    void register_success() throws Exception {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void register_duplicateUsername_conflict() throws Exception {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setUsername("dupuser");
        req.setEmail("dup@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest.Register dup = new AuthRequest.Register();
        dup.setUsername("dupuser");
        dup.setEmail("other@example.com");
        dup.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateEmail_conflict() throws Exception {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setUsername("user1");
        req.setEmail("shared@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest.Register dup = new AuthRequest.Register();
        dup.setUsername("user2");
        dup.setEmail("shared@example.com");
        dup.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_badRequest() throws Exception {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setUsername("user2");
        req.setEmail("not-an-email");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void register_shortPassword_badRequest() throws Exception {
        AuthRequest.Register req = new AuthRequest.Register();
        req.setUsername("user3");
        req.setEmail("user3@example.com");
        req.setPassword("abc");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_success() throws Exception {
        AuthRequest.Register reg = new AuthRequest.Register();
        reg.setUsername("loginuser");
        reg.setEmail("login@example.com");
        reg.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("loginuser");
        login.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    void login_wrongPassword_unauthorized() throws Exception {
        AuthRequest.Register reg = new AuthRequest.Register();
        reg.setUsername("authuser");
        reg.setEmail("auth@example.com");
        reg.setPassword("correctpass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("authuser");
        login.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_unauthorized() throws Exception {
        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("nobody");
        login.setPassword("doesntmatter");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_forbidden() throws Exception {
        mockMvc.perform(post("/api/mailing-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
