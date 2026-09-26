package com.zpantry.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zpantry.ZPantryBackendApplication;
import com.zpantry.foundation.IsolatedPostgres;
import com.zpantry.user.persistence.UserRepository;
import com.zpantry.user.security.TokenRevocationChecker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = ZPantryBackendApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never",
        "zpantry.security.jwt.enabled=true",
        "zpantry.security.jwt.secret=TEST_ONLY_User_slice_signing_key_2026_at_least_32_bytes",
        "zpantry.security.jwt.issuer=", "zpantry.security.jwt.audience="})
@AutoConfigureMockMvc
@Import({IsolatedPostgres.class, UserSliceIT.SecurityTestConfiguration.class})
class UserSliceIT {
    private static final String TEST_SECRET = "TEST_ONLY_User_slice_signing_key_2026_at_least_32_bytes";
    private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000003");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM users");
        insert(OWNER, "Owner", "owner@test.dev", "owner-hash", "123456", "owner-refresh", true, true, "user");
        insert(OTHER, "Other", "other@test.dev", "other-hash", "234567", "other-refresh", true, true, "user");
        insert(ADMIN, "Admin", "admin@test.dev", "admin-hash", "345678", "admin-refresh", true, true, "admin");
    }

    @Test
    void adminListUsesOneBasedNormalizationClampAndEmailOrder() throws Exception {
        mvc.perform(get("/api/users?pageIndex=0&pageSize=101").with(adminJwt()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.pageSize").value(100)).andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.data[0].email").value("admin@test.dev"))
                .andExpect(jsonPath("$.data[2].email").value("owner@test.dev"))
                .andExpect(jsonPath("$.data[0].passwordHashed").doesNotExist());
    }

    @Test
    void adminDetailMissingAndDeletePreserveLegacyStatuses() throws Exception {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000099");
        mvc.perform(get("/api/users/{id}", missing).with(adminJwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").value("User not found."));
        mvc.perform(delete("/api/users/{id}", OWNER).with(adminJwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(delete("/api/users/{id}", OWNER).with(adminJwt())).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
        var row = jdbc.queryForMap("SELECT * FROM users WHERE id=?", OWNER);
        assertThat(row.get("is_deleted")).isEqualTo(true);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("updated_at")).isNull();
        assertThat(row.get("password_hashed")).isEqualTo("owner-hash");
        assertThat(row.get("refresh_token_hash")).isEqualTo("owner-refresh");
    }

    @Test
    void approvedOwnerPutAllowsUserIdDespiteEmailSubjectAndPreservesSensitiveState() throws Exception {
        mvc.perform(put("/api/users/{id}", OWNER).with(ownerJwt(OWNER))
                        .contentType("application/json").content("{\"fullName\":\"\",\"avatarUrl\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value(""));
        var row = jdbc.queryForMap("SELECT * FROM users WHERE id=?", OWNER);
        assertThat(row.get("updated_at")).isNotNull();
        assertThat(row.get("password_hashed")).isEqualTo("owner-hash");
        assertThat(row.get("otp_code")).isEqualTo("123456");
        assertThat(row.get("refresh_token_hash")).isEqualTo("owner-refresh");
        assertThat(row.get("role")).isEqualTo("user");
    }

    @Test
    void emptyAndPasswordUpdatesHaveExactMutationBoundaries() throws Exception {
        mvc.perform(put("/api/users/{id}", OWNER).with(ownerJwt(OWNER))
                .contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        Instant afterEmpty = jdbc.queryForObject("SELECT updated_at FROM users WHERE id=?", Instant.class, OWNER);
        mvc.perform(put("/api/users/{id}", OWNER).with(ownerJwt(OWNER))
                .contentType("application/json").content("{\"password\":\"Synthetic-New-Password!\"}"))
                .andExpect(status().isOk());
        var row = jdbc.queryForMap("SELECT * FROM users WHERE id=?", OWNER);
        Instant afterPassword = jdbc.queryForObject("SELECT updated_at FROM users WHERE id=?", Instant.class, OWNER);
        assertThat(afterPassword).isAfterOrEqualTo(afterEmpty);
        assertThat(row.get("password_hashed")).isNotEqualTo("owner-hash");
        assertThat(row.get("full_name")).isEqualTo("Owner");
        assertThat(row.get("otp_code")).isEqualTo("123456");
        assertThat(row.get("refresh_token_hash")).isEqualTo("owner-refresh");
    }

    @Test
    void wrongOwnerAdminOtherAndMissingIdentityAreForbidden() throws Exception {
        mvc.perform(put("/api/users/{id}", OTHER).with(ownerJwt(OWNER)).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/api/users/{id}", OWNER).with(adminJwt()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/{id}", OWNER).with(jwt().jwt(token -> token.claim("sub", "owner@test.dev").claim("jti", "x")))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void middlewareProducesLegacyUnauthorizedAndForbiddenBoundaries() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(content().string(""));
        mvc.perform(get("/api/users").with(ownerJwt(OWNER))).andExpect(status().isForbidden()).andExpect(content().string(""));
        mvc.perform(get("/api/users/{id}", OWNER).with(ownerJwt(OWNER))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/{id}", OWNER).with(ownerJwt(OWNER))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/{id}", OWNER)).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesARealHs256LegacyCompatibleBearerToken() throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .subject("admin@test.dev").claim("email", "admin@test.dev")
                .claim("userId", ADMIN.toString()).claim("role", "admin")
                .jwtID("signed-test-jti").issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1))).expirationTime(Date.from(now.plusSeconds(60))).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        token.sign(new MACSigner(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token.serialize()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void exactVerifiedColumnsAreMappedAndSoftDeletedRowsAreExcluded() {
        var columns = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name='users' ORDER BY ordinal_position", String.class);
        assertThat(columns).containsExactly("id", "created_at", "created_by", "updated_at", "updated_by",
                "deleted_at", "deleted_by", "is_deleted", "full_name", "email", "avatar_url",
                "password_hashed", "otp_code", "otp_expired_at", "otp_retry_count",
                "is_email_confirmed", "is_active", "role", "refresh_token_hash", "refresh_token_expires_at");
        jdbc.update("UPDATE users SET is_deleted=true, deleted_at=now() WHERE id=?", OTHER);
        assertThat(users.findByIdAndDeletedFalse(OTHER)).isEmpty();
        assertThat(users.countByDeletedFalse()).isEqualTo(2);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor ownerJwt(UUID id) {
        return jwt().jwt(token -> token.claim("sub", "owner@test.dev").claim("userId", id.toString())
                .claim("role", "user").claim("jti", "synthetic-jti"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.claim("sub", "admin@test.dev").claim("userId", ADMIN.toString())
                        .claim("role", "admin").claim("jti", "synthetic-admin-jti"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private void insert(UUID id, String name, String email, String hash, String otp, String refresh,
            boolean confirmed, boolean active, String role) {
        jdbc.update("""
                INSERT INTO users(id,created_at,full_name,email,password_hashed,otp_code,otp_retry_count,
                    is_email_confirmed,is_active,role,refresh_token_hash,is_deleted)
                VALUES (?,now(),?,?,?,?,0,?,?,?,?,false)
                """, id, name, email, hash, otp, confirmed, active, role, refresh);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityTestConfiguration {
        @Bean TokenRevocationChecker tokenRevocationChecker() { return jti -> false; }
    }
}
