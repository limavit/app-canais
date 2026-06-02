package com.example.iptvmanager.iptvlist;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;
import com.example.iptvmanager.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IptvListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateListFromUrl() throws Exception {
        String token = registerAndToken("list-url@example.com");

        mockMvc.perform(post("/api/iptv-lists/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Minha lista",
                                  "description": "Lista autorizada",
                                  "sourceUrl": "https://example.com/list.m3u8"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Minha lista"))
                .andExpect(jsonPath("$.sourceType").value("URL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalChannels").value(0));
    }

    @Test
    void shouldRejectInvalidUrl() throws Exception {
        String token = registerAndToken("invalid-url@example.com");

        mockMvc.perform(post("/api/iptv-lists/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lista invalida",
                                  "sourceUrl": "ftp://example.com/list.m3u8"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateListFromUpload() throws Exception {
        String token = registerAndToken("upload@example.com");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.m3u",
                "audio/x-mpegurl",
                """
                        #EXTM3U
                        #EXTINF:-1,Canal Teste
                        https://example.com/stream/test.m3u8
                        """.getBytes()
        );

        mockMvc.perform(multipart("/api/iptv-lists/upload")
                        .file(file)
                        .param("name", "Upload M3U")
                        .param("description", "Arquivo autorizado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("FILE"))
                .andExpect(jsonPath("$.originalFileName").value("sample.m3u"))
                .andExpect(jsonPath("$.sourceUrl", notNullValue()));
    }

    @Test
    void shouldKeepListsIsolatedByOwner() throws Exception {
        String ownerToken = registerAndToken("owner@example.com");
        String otherToken = registerAndToken("other@example.com");

        String response = createUrlList(ownerToken, "Lista privada");
        Long listId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/iptv-lists/" + listId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/iptv-lists")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldAllowAdminToSeeAllLists() throws Exception {
        String ownerToken = registerAndToken("visible-owner@example.com");
        createUrlList(ownerToken, "Lista visivel");

        String adminToken = createAdminAndLogin("admin-lists@example.com");

        mockMvc.perform(get("/api/iptv-lists")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Lista visivel"));
    }

    private String registerAndToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Usuario Lista",
                                  "email": "%s",
                                  "password": "123456"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createUrlList(String token, String name) throws Exception {
        return mockMvc.perform(post("/api/iptv-lists/url")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "sourceUrl": "https://example.com/list.m3u8"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String createAdminAndLogin(String email) throws Exception {
        User admin = new User();
        admin.setName("Admin Listas");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "123456"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }
}
