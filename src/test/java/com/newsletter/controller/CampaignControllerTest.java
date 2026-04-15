package com.newsletter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsletter.dto.request.AuthRequest;
import com.newsletter.dto.request.CampaignRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    private String token;
    private String mailingListId;

    @BeforeEach
    void setup() throws Exception {
        AuthRequest.Register reg = new AuthRequest.Register();
        reg.setUsername("campuser");
        reg.setEmail("camp@example.com");
        reg.setPassword("pass1234");

        MvcResult r = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        token = objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();

        // Create a mailing list to use in tests
        MailingListRequest.Create listReq = new MailingListRequest.Create();
        listReq.setName("Test Campaign List");
        MvcResult listResult = mockMvc.perform(post("/api/mailing-lists")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listReq)))
                .andReturn();
        mailingListId = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("id").asText();
    }

    @AfterEach
    void cleanup() {
        mongoTemplate.getDb().drop();
    }

    private String auth() { return "Bearer " + token; }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createCampaign_asDraft() throws Exception {
        CampaignRequest.Create req = buildCreate("Spring Sale", "Big Spring Sale!", "Check our deals.");

        mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Spring Sale"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createCampaign_missingName_badRequest() throws Exception {
        CampaignRequest.Create req = new CampaignRequest.Create();
        req.setSubject("Subject");
        req.setContent("Content");

        mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Test
    void getCampaignById_success() throws Exception {
        String id = createDraft("Get Me Campaign");

        mockMvc.perform(get("/api/campaigns/" + id).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Get Me Campaign"));
    }

    @Test
    void getCampaignById_notFound_404() throws Exception {
        mockMvc.perform(get("/api/campaigns/nonexistentid").header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void updateCampaign_success() throws Exception {
        String id = createDraft("Original Name");

        CampaignRequest.Update update = new CampaignRequest.Update();
        update.setName("Updated Name");
        update.setSubject("Updated Subject");

        mockMvc.perform(put("/api/campaigns/" + id)
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.subject").value("Updated Subject"));
    }

    @Test
    void updateCampaign_partialUpdate_onlyChangesProvidedFields() throws Exception {
        String id = createDraft("Keep My Name");

        CampaignRequest.Update update = new CampaignRequest.Update();
        update.setSubject("Only Subject Changed");
        // name not provided — should be preserved

        mockMvc.perform(put("/api/campaigns/" + id)
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keep My Name"))
                .andExpect(jsonPath("$.subject").value("Only Subject Changed"));
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    @Test
    void scheduleCampaign_success() throws Exception {
        String id = createDraft("To Schedule");

        CampaignRequest.Schedule schedule = new CampaignRequest.Schedule();
        schedule.setScheduledAt(LocalDateTime.now().plusDays(1));
        schedule.setMailingListId(mailingListId);

        mockMvc.perform(post("/api/campaigns/" + id + "/schedule")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.mailingListId").value(mailingListId));
    }

    @Test
    void scheduleCampaign_pastDate_badRequest() throws Exception {
        String id = createDraft("Past Schedule");

        CampaignRequest.Schedule schedule = new CampaignRequest.Schedule();
        schedule.setScheduledAt(LocalDateTime.now().minusHours(1));
        schedule.setMailingListId(mailingListId);

        mockMvc.perform(post("/api/campaigns/" + id + "/schedule")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedule)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleCampaign_missingMailingList_badRequest() throws Exception {
        String id = createDraft("No List");

        CampaignRequest.Schedule schedule = new CampaignRequest.Schedule();
        schedule.setScheduledAt(LocalDateTime.now().plusDays(1));
        // mailingListId omitted

        mockMvc.perform(post("/api/campaigns/" + id + "/schedule")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedule)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.mailingListId").exists());
    }

    // ── Pagination and filtering ───────────────────────────────────────────────

    @Test
    void getAllCampaigns_pagination() throws Exception {
        createDraft("Camp A");
        createDraft("Camp B");
        createDraft("Camp C");
        createDraft("Camp D");
        createDraft("Camp E");

        mockMvc.perform(get("/api/campaigns?page=0&size=3").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/campaigns?page=1&size=3").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getAllCampaigns_filterByStatus() throws Exception {
        String id = createDraft("Will Be Scheduled");
        createDraft("Stays Draft");

        CampaignRequest.Schedule schedule = new CampaignRequest.Schedule();
        schedule.setScheduledAt(LocalDateTime.now().plusDays(1));
        schedule.setMailingListId(mailingListId);
        mockMvc.perform(post("/api/campaigns/" + id + "/schedule")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedule)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/campaigns?status=SCHEDULED").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"));

        mockMvc.perform(get("/api/campaigns?status=DRAFT").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteCampaign_success() throws Exception {
        String id = createDraft("To Delete");

        mockMvc.perform(delete("/api/campaigns/" + id).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/campaigns/" + id).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCampaign_notFound_404() throws Exception {
        mockMvc.perform(delete("/api/campaigns/doesnotexist").header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    // ── Ownership isolation ───────────────────────────────────────────────────

    @Test
    void campaign_isolatedBetweenUsers() throws Exception {
        String id = createDraft("User1 Private Campaign");

        AuthRequest.Register reg2 = new AuthRequest.Register();
        reg2.setUsername("other");
        reg2.setEmail("other@example.com");
        reg2.setPassword("pass1234");

        MvcResult r2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg2)))
                .andReturn();
        String token2 = "Bearer " + objectMapper.readTree(r2.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/campaigns/" + id).header("Authorization", token2))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createDraft(String name) throws Exception {
        CampaignRequest.Create req = buildCreate(name, "Subject of " + name, "Content body here.");
        MvcResult r = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private CampaignRequest.Create buildCreate(String name, String subject, String content) {
        CampaignRequest.Create req = new CampaignRequest.Create();
        req.setName(name);
        req.setSubject(subject);
        req.setContent(content);
        req.setMailingListId(mailingListId);
        return req;
    }
}
