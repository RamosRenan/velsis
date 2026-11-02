package com.velsis.monitoring.security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class AuthVerifyController {
    private static final Logger log = LoggerFactory.getLogger(AuthVerifyController.class);

    @GetMapping("auth-verify")
    public boolean csrf(Authentication authentication) {
        log.info("isAuthenticated "+authentication.isAuthenticated());
        return authentication.isAuthenticated();
    }
}
