package com.smile.usermanagement.web;

public record CaptchaResponse(
    String captchaId,
    String code
) {}

