package com.library;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 关键接口集成测试：真实 MySQL + Flyway 迁移 + 种子数据。
 * 种子数据只包含角色字典与管理员账号，业务数据全部由测试用例现场创建。
 * 需要 Docker 环境，执行：mvn -Pit verify
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("library")
            .withUsername("library")
            .withPassword("library123");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanLoginAndReadMe() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String accessToken = read(body).path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    @Test
    void readerCanSelfRegisterAndReadOwnProfile() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"selfreader\",\"password\":\"Reader@123456\","
                                + "\"displayName\":\"自助注册读者\",\"phone\":\"13900001111\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.roles[0]").value("READER"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String token = read(body).path("data").path("accessToken").asText();

        // 注册即开通借阅证，读者能读取自己的档案
        mockMvc.perform(get("/api/v1/readers/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("自助注册读者"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.maxBorrow").value(5))
                .andExpect(jsonPath("$.data.cardNo").value(org.hamcrest.Matchers.startsWith("R")));

        // 自助注册的账号只有 READER 权限
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 重复用户名注册被拒绝
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"selfreader\",\"password\":\"Reader@123456\","
                                + "\"displayName\":\"重复账号\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"123\",\"displayName\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42200));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"bad-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void demoDataIsNotSeeded() throws Exception {
        String token = login("admin", "Admin@123456");

        // 全新库只有管理员账号和角色字典，业务数据为空
        mockMvc.perform(get("/api/v1/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/readers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/authors").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void librarianCanNotAccessUserManagement() throws Exception {
        String adminToken = login("admin", "Admin@123456");
        createUser(adminToken, "librarian_test", "Lib@123456", "LIBRARIAN");

        String token = login("librarian_test", "Lib@123456");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void refreshTokenIsRotated() throws Exception {
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String refreshToken = read(loginBody).path("data").path("refreshToken").asText();

        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String newRefreshToken = read(refreshBody).path("data").path("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 旧刷新令牌已被轮换，重复使用应当失败
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    private void createUser(String adminToken, String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                                + "\",\"displayName\":\"测试账号\",\"roles\":[\"" + role + "\"]}"))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return read(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("accessToken").asText();
    }

    private JsonNode read(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
