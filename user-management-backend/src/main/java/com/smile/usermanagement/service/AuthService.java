package com.smile.usermanagement.service;

import com.smile.usermanagement.web.AuthLoginResponse;
import com.smile.usermanagement.web.CaptchaResponse;

public interface AuthService {

    CaptchaResponse createCaptcha();

    AuthLoginResponse login(String username, String password, String captchaId, String captcha);

    AuthLoginResponse me(Long userId);

    String secondVerify(Long userId, String password);

    void assertSecondVerified(Long userId, String token);
}

