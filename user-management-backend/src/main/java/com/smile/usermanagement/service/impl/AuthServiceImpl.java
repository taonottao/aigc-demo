package com.smile.usermanagement.service.impl;

import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.security.JwtTokenService;
import com.smile.usermanagement.service.AuditService;
import com.smile.usermanagement.service.AuthService;
import com.smile.usermanagement.service.PermissionService;
import com.smile.usermanagement.web.AuthLoginResponse;
import com.smile.usermanagement.web.CaptchaResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final long CAPTCHA_TTL_SECONDS = 180;
    private static final long SECOND_VERIFY_TTL_SECONDS = 300;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final HttpServletRequest request;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaItem> captchas = new ConcurrentHashMap<>();
    private final Map<Long, SecondVerifyItem> secondVerifyTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                           PermissionService permissionService, AuditService auditService, HttpServletRequest request) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.request = request;
    }

    @Override
    public CaptchaResponse createCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = randomCode(4);
        captchas.put(captchaId, new CaptchaItem(code, Instant.now().plusSeconds(CAPTCHA_TTL_SECONDS)));
        return new CaptchaResponse(captchaId, code);
    }

    @Override
    public AuthLoginResponse login(String username, String password, String captchaId, String captcha) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Username/password required");
        }
        verifyCaptcha(captchaId, captcha);

        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
            .eq(User::getUsername, username));
        if (user == null) {
            auditService.loginLog(null, username, false, clientIp(), userAgent(), "Invalid username/password");
            throw new IllegalArgumentException("Invalid username/password");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            auditService.loginLog(user.getId(), username, false, clientIp(), userAgent(), "User disabled");
            throw new IllegalArgumentException("User disabled");
        }

        if (!matchesAndUpgrade(user, password)) {
            auditService.loginLog(user.getId(), username, false, clientIp(), userAgent(), "Invalid username/password");
            throw new IllegalArgumentException("Invalid username/password");
        }

        auditService.loginLog(user.getId(), username, true, clientIp(), userAgent(), "OK");
        return buildLoginResponse(user);
    }

    @Override
    public AuthLoginResponse me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return buildLoginResponse(user);
    }

    @Override
    public String secondVerify(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (!matchesAndUpgrade(user, password)) {
            throw new IllegalArgumentException("Password incorrect");
        }
        String token = UUID.randomUUID().toString();
        secondVerifyTokens.put(userId, new SecondVerifyItem(token, Instant.now().plusSeconds(SECOND_VERIFY_TTL_SECONDS)));
        return token;
    }

    @Override
    public void assertSecondVerified(Long userId, String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Second verification required");
        }
        SecondVerifyItem item = secondVerifyTokens.get(userId);
        if (item == null || item.expiresAt().isBefore(Instant.now()) || !token.equals(item.token())) {
            throw new IllegalArgumentException("Second verification invalid/expired");
        }
    }

    private AuthLoginResponse buildLoginResponse(User user) {
        String token = jwtTokenService.issueToken(user.getId(), user.getUsername());
        return new AuthLoginResponse(
            token,
            AuthLoginResponse.UserInfo.from(user),
            permissionService.listUserMenus(user.getId()),
            permissionService.listUserPermissions(user.getId())
        );
    }

    private void verifyCaptcha(String captchaId, String captcha) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captcha)) {
            throw new IllegalArgumentException("Captcha required");
        }
        CaptchaItem item = captchas.remove(captchaId);
        if (item == null || item.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Captcha expired");
        }
        if (!item.code().equalsIgnoreCase(captcha.trim())) {
            throw new IllegalArgumentException("Captcha incorrect");
        }
    }

    private boolean matchesAndUpgrade(User user, String rawPassword) {
        String stored = user.getPassword();
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        boolean isBcrypt = stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
        boolean matched = isBcrypt ? passwordEncoder.matches(rawPassword, stored) : rawPassword.equals(stored);
        if (matched && !isBcrypt) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(user);
        }
        return matched;
    }

    private String randomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent() {
        String ua = request.getHeader("User-Agent");
        return StringUtils.hasText(ua) ? ua.substring(0, Math.min(255, ua.length())) : null;
    }

    private record CaptchaItem(String code, Instant expiresAt) {}

    private record SecondVerifyItem(String token, Instant expiresAt) {}
}

