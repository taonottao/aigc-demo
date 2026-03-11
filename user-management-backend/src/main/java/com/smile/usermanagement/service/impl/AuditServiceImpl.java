package com.smile.usermanagement.service.impl;

import com.smile.usermanagement.entity.LoginLog;
import com.smile.usermanagement.entity.OperationLog;
import com.smile.usermanagement.mapper.LoginLogMapper;
import com.smile.usermanagement.mapper.OperationLogMapper;
import com.smile.usermanagement.service.AuditService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;

    public AuditServiceImpl(LoginLogMapper loginLogMapper, OperationLogMapper operationLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void loginLog(Long userId, String username, boolean success, String ip, String userAgent, String message) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setSuccess(success);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    @Override
    public void opLog(Long userId, String username, String module, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}

