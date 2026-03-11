package com.smile.usermanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.entity.LoginLog;
import com.smile.usermanagement.entity.OperationLog;
import com.smile.usermanagement.mapper.LoginLogMapper;
import com.smile.usermanagement.mapper.OperationLogMapper;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;

    public LogController(LoginLogMapper loginLogMapper, OperationLogMapper operationLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping("/login")
    @PreAuthorize("hasAuthority('security:view')")
    public List<LoginLog> loginLogs() {
        return loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
            .orderByDesc(LoginLog::getCreatedAt)
            .last("limit 200"));
    }

    @GetMapping("/operations")
    @PreAuthorize("hasAuthority('security:view')")
    public List<OperationLog> opLogs() {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
            .orderByDesc(OperationLog::getCreatedAt)
            .last("limit 200"));
    }
}

