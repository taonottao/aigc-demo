package com.smile.usermanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smile.usermanagement.entity.LoginLog;
import com.smile.usermanagement.entity.OperationLog;
import com.smile.usermanagement.mapper.LoginLogMapper;
import com.smile.usermanagement.mapper.OperationLogMapper;
import com.smile.usermanagement.web.PageResult;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/login/page")
    @PreAuthorize("hasAuthority('security:view')")
    public PageResult<LoginLog> loginLogsPage(@RequestParam(defaultValue = "1") Long page,
                                              @RequestParam(defaultValue = "20") Long size) {
        long current = page <= 0 ? 1 : page;
        long pageSize = size <= 0 ? 20 : Math.min(size, 200);
        return PageResult.from(loginLogMapper.selectPage(
            new Page<>(current, pageSize),
            new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getCreatedAt)
        ));
    }

    @GetMapping("/operations")
    @PreAuthorize("hasAuthority('security:view')")
    public List<OperationLog> opLogs() {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
            .orderByDesc(OperationLog::getCreatedAt)
            .last("limit 200"));
    }

    @GetMapping("/operations/page")
    @PreAuthorize("hasAuthority('security:view')")
    public PageResult<OperationLog> opLogsPage(@RequestParam(defaultValue = "1") Long page,
                                               @RequestParam(defaultValue = "20") Long size) {
        long current = page <= 0 ? 1 : page;
        long pageSize = size <= 0 ? 20 : Math.min(size, 200);
        return PageResult.from(operationLogMapper.selectPage(
            new Page<>(current, pageSize),
            new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreatedAt)
        ));
    }
}
