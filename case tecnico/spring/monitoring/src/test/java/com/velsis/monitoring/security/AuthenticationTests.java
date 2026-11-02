package com.velsis.monitoring.security;

import com.velsis.monitoring.security.config.MonitoringSecurityConfig;
import com.velsis.monitoring.security.controller.CsrfController;
import com.velsis.monitoring.security.persistence.entities.MonitoringUser;
import com.velsis.monitoring.security.persistence.repository.UserRepository;
import com.velsis.monitoring.security.service.DatabaseUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(AuthenticationTests.Config.class)
class AuthenticationTests {
    @Configuration
    @EnableWebMvc
    @Import({MonitoringSecurityConfig.class, DatabaseUserDetailsService.class, CsrfController.class, Probe.class})
    static class Config {
        @Bean
        UserRepository users() {
            return mock(UserRepository.class);
        }
    }

    @RestController
    static class Probe {
        @GetMapping({"/api/public/probe", "/api/private/probe"})
        String probe() {
            return "ok";
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(users);
        var user = mock(MonitoringUser.class);
        when(user.getUsuario()).thenReturn("teste");
        when(user.getSenha()).thenReturn(encoder.encode("senha-correta"));
        when(users.findById("teste")).thenReturn(Optional.of(user));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicAccessAndPrivateProtection() throws Exception {
        mvc.perform(get("/api/public/probe")).andExpect(status().isOk());
        mvc.perform(get("/api/public/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(get("/api/private/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginUsesDatabasePasswordAndLogoutInvalidatesSession() throws Exception {
        var result = mvc.perform(post("/api/public/login").with(csrf())
                        .param("usuario", "teste").param("senha", "senha-correta"))
                .andExpect(status().isNoContent()).andExpect(authenticated().withUsername("teste"))
                .andReturn();
        verify(users).findById("teste");
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/private/probe").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(unauthenticated())
                .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/private/probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUser() throws Exception {
        mvc.perform(post("/api/public/login").with(csrf())
                        .param("usuario", "teste").param("senha", "incorreta"))
                .andExpect(status().isUnauthorized()).andExpect(unauthenticated());
        mvc.perform(post("/api/public/login").with(csrf())
                        .param("usuario", "inexistente").param("senha", "senha-correta"))
                .andExpect(status().isUnauthorized()).andExpect(unauthenticated());
    }

    @Test
    void requiresCsrfForLoginAndLogout() throws Exception {
        mvc.perform(post("/api/public/login").param("usuario", "teste").param("senha", "senha-correta"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/logout")).andExpect(status().isForbidden());
    }
}
