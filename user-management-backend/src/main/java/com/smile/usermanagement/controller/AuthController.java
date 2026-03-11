package com.smile.usermanagement.controller;

import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.security.UserPrincipal;
import com.smile.usermanagement.service.AuthService;
import com.smile.usermanagement.web.AuthLoginRequest;
import com.smile.usermanagement.web.AuthLoginResponse;
import com.smile.usermanagement.web.CaptchaResponse;
import com.smile.usermanagement.web.SecondVerifyRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/captcha")
    public CaptchaResponse captcha() {
        return authService.createCaptcha();
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword(), request.getCaptchaId(), request.getCaptcha());
    }

    @GetMapping("/me")
    public AuthLoginResponse me() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return authService.me(principal.id());
    }

    @PostMapping("/second-verify")
    public Map<String, Object> secondVerify(@Valid @RequestBody SecondVerifyRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        if (principal == null) {
            throw new IllegalArgumentException("Not authenticated");
        }
        String token = authService.secondVerify(principal.id(), request.getPassword());
        return Map.of("token", token);
    }
}

