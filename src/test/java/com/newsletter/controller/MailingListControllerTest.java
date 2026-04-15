package com.newsletter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsletter.dto.request.AuthRequest;
import com.newsletter.dto.request.MailingListRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailingListControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    private String token;

    @BeforeEach
    void setup() throws Exception {
        AuthRequest.Register reg = new AuthRequest.Register();
        reg.setUsername("mluser");
        reg.setEmail("ml@example.com");
        reg.setPassword("pass1234");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().drop();
    }

    private String auth() { return "Bearer " + token; }

    // ── Mailing List CRUD ─────────────────────────────────────────────────────

    @Test
    void createMailingList_success() throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        req.setName("Weekly Digest");
        req.setDescription("Our weekly update");

        mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Weekly Digest"))
                .andExpect(jsonPath("$.subscriberCount").value(0))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createMailingList_missingName_badRequest() throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        // name deliberately omitted

        mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createMailingList_duplicateName_conflict() throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        req.setName("Duplicate List");

        mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllMailingLists_success() throws Exception {
        for (String name : new String[]{"List A", "List B", "List C"}) {
            MailingListRequest.Create req = new MailingListRequest.Create();
            req.setName(name);
            mockMvc.perform(post("/api/mailing-lists")
                            .header("Authorization", auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/mailing-lists").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getMailingListById_success() throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        req.setName("Detail List");
        req.setDescription("With description");

        MvcResult result = mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/mailing-lists/" + id).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Detail List"))
                .andExpect(jsonPath("$.description").value("With description"))
                .andExpect(jsonPath("$.subscribers").isArray());
    }

    @Test
    void getMailingListById_notFound_404() throws Exception {
        mockMvc.perform(get("/api/mailing-lists/nonexistentid").header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMailingList_success() throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        req.setName("To Delete");

        MvcResult result = mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/mailing-lists/" + id).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/mailing-lists/" + id).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    // ── Subscriber management ─────────────────────────────────────────────────

    @Test
    void addSubscriber_success() throws Exception {
        String listId = createList("Subscriber List");

        MailingListRequest.AddSubscriber sub = new MailingListRequest.AddSubscriber();
        sub.setName("Alice Smith");
        sub.setEmail("alice@example.com");

        mockMvc.perform(post("/api/mailing-lists/" + listId + "/subscribers")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void addSubscriber_duplicateEmail_conflict() throws Exception {
        String listId = createList("Dup Sub List");

        MailingListRequest.AddSubscriber sub = new MailingListRequest.AddSubscriber();
        sub.setName("Bob");
        sub.setEmail("bob@example.com");

        mockMvc.perform(post("/api/mailing-lists/" + listId + "/subscribers")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/mailing-lists/" + listId + "/subscribers")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isConflict());
    }

    @Test
    void addSubscriber_invalidEmail_badRequest() throws Exception {
        String listId = createList("Validation List");

        MailingListRequest.AddSubscriber sub = new MailingListRequest.AddSubscriber();
        sub.setName("Bad Email");
        sub.setEmail("not-valid");

        mockMvc.perform(post("/api/mailing-lists/" + listId + "/subscribers")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void removeSubscriber_success() throws Exception {
        String listId = createList("Remove Sub List");

        MailingListRequest.AddSubscriber sub = new MailingListRequest.AddSubscriber();
        sub.setName("Charlie");
        sub.setEmail("charlie@example.com");

        MvcResult subResult = mockMvc.perform(post("/api/mailing-lists/" + listId + "/subscribers")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated())
                .andReturn();

        String subId = objectMapper.readTree(subResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/mailing-lists/" + listId + "/subscribers/" + subId)
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify subscriber count is 0
        mockMvc.perform(get("/api/mailing-lists/" + listId).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberCount").value(0));
    }

    @Test
    void mailingList_isolatedBetweenUsers() throws Exception {
        // Register a second user
        AuthRequest.Register reg2 = new AuthRequest.Register();
        reg2.setUsername("otheruser");
        reg2.setEmail("other@example.com");
        reg2.setPassword("pass1234");

        MvcResult r2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andReturn();
        String token2 = "Bearer " + objectMapper.readTree(r2.getResponse().getContentAsString()).get("token").asText();

        // User 1 creates a list
        String listId = createList("User1 Private List");

        // User 2 cannot access it
        mockMvc.perform(get("/api/mailing-lists/" + listId).header("Authorization", token2))
                .andExpect(status().isNotFound());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String createList(String name) throws Exception {
        MailingListRequest.Create req = new MailingListRequest.Create();
        req.setName(name);
        MvcResult result = mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
