package com.learningsystemserver.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningsystemserver.dtos.requests.TopicRequest;
import com.learningsystemserver.dtos.responses.TopicResponse;
import com.learningsystemserver.security.SecurityConfig;
import com.learningsystemserver.services.JwtService;
import com.learningsystemserver.services.TopicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@Import(SecurityConfig.class)
class TopicControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void userCannotUseTopicManagementEndpoints() throws Exception {
        String requestJson = objectMapper.writeValueAsString(topicRequest());

        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/topics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/topics/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/topics/deleted"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/topics/1/restore"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(topicService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUseTopicManagementEndpoints() throws Exception {
        TopicResponse topic = topicResponse();
        when(topicService.createTopic(any(TopicRequest.class))).thenReturn(topic);
        when(topicService.updateTopic(eq(1L), any(TopicRequest.class))).thenReturn(topic);
        when(topicService.getDeletedTopics()).thenReturn(List.of(topic));
        when(topicService.restoreTopic(1L)).thenReturn(topic);

        String requestJson = objectMapper.writeValueAsString(topicRequest());

        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/topics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/topics/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/topics/deleted"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/topics/1/restore"))
                .andExpect(status().isOk());

        verify(topicService).deleteTopic(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanReadTopics() throws Exception {
        TopicResponse topic = topicResponse();
        when(topicService.getTopLevelTopics()).thenReturn(List.of(topic));
        when(topicService.getSubTopics(1L)).thenReturn(List.of(topic));
        when(topicService.getTopic(1L)).thenReturn(topic);

        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/topics").param("parentId", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/topics/1"))
                .andExpect(status().isOk());
    }

    private static TopicRequest topicRequest() {
        TopicRequest request = new TopicRequest();
        request.setName("Algebra");
        request.setDescription("Linear equations");
        return request;
    }

    private static TopicResponse topicResponse() {
        TopicResponse response = new TopicResponse();
        response.setId(1L);
        response.setName("Algebra");
        response.setDescription("Linear equations");
        return response;
    }
}
