package com.smile.usermanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smile.usermanagement.dto.RoleSimple;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.mapper.RoleMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleMapper roleMapper;

    public RoleController(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @GetMapping("/simple")
    public List<RoleSimple> listSimple() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
            .eq(Role::getStatus, 1)
            .orderByAsc(Role::getId);
        List<Role> roles = roleMapper.selectList(wrapper);
        return roles.stream()
            .map(role -> new RoleSimple(role.getId(), role.getName(), role.getCode()))
            .collect(Collectors.toList());
    }
}
