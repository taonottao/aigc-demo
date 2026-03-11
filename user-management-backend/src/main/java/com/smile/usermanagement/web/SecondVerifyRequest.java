package com.smile.usermanagement.web;

import jakarta.validation.constraints.NotBlank;

public class SecondVerifyRequest {

    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

