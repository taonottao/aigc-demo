package com.smile.usermanagement.service;

public interface AuditService {

    void loginLog(Long userId, String username, boolean success, String ip, String userAgent, String message);

    void opLog(Long userId, String username, String module, String action, String detail);
}

