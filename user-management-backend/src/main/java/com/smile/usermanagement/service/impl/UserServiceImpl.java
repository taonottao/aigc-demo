package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.mapper.UserRoleMapper;
import com.smile.usermanagement.security.SecurityUtils;
import com.smile.usermanagement.service.UserService;
import com.smile.usermanagement.service.DataScopeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final DataScopeService dataScopeService;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserRoleMapper userRoleMapper, DataScopeService dataScopeService) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleMapper = userRoleMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public List<User> listByOrgAndKeyword(Long orgId, String keyword) {
        var principal = SecurityUtils.getCurrentUser();
        if (principal != null && orgId != null) {
            DataScopeService.Scope scope = dataScopeService.resolveUserModuleScope(principal.id());
            if (scope != DataScopeService.Scope.ALL) {
                Set<Long> accessibleOrgIds = dataScopeService.resolveAccessibleOrgIds(principal.id());
                if (scope == DataScopeService.Scope.ORG_ONLY || scope == DataScopeService.Scope.ORG_AND_CHILDREN) {
                    if (!accessibleOrgIds.contains(orgId)) {
                        throw new AccessDeniedException("No data permission for orgId=" + orgId);
                    }
                }
            }
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(User::getOrgId, orgId);
        }
        if (principal != null && dataScopeService.resolveUserModuleScope(principal.id()) == DataScopeService.Scope.SELF_ONLY) {
            wrapper.eq(User::getId, principal.id());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getRealName, keyword)
                .or().like(User::getPhone, keyword)
                .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getId);
        return list(wrapper);
    }

    @Override
    public User createUser(User user, List<Long> roleIds) {
        validateUsernameUnique(user.getUsername(), null);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
        user.setPassword(encodeIfNeeded(user.getPassword()));
        save(user);
        if (roleIds != null) {
            replaceRoles(user.getId(), roleIds);
        }
        return user;
    }

    @Override
    public User updateUser(Long id, User user, List<Long> roleIds) {
        User existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        validateUsernameUnique(user.getUsername(), id);

        existing.setUsername(user.getUsername());
        if (StringUtils.hasText(user.getPassword())) {
            existing.setPassword(encodeIfNeeded(user.getPassword()));
        }
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setAvatar(user.getAvatar());
        existing.setOrgId(user.getOrgId());
        existing.setStatus(user.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        if (roleIds != null) {
            replaceRoles(id, roleIds);
        }
        return existing;
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password is required");
        }
        existing.setPassword(passwordEncoder.encode(newPassword));
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
    }

    @Override
    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            userRoleMapper.insert(userId, roleId);
        }
    }

    @Override
    @Transactional
    public boolean deleteUserById(Long userId) {
        User existing = getById(userId);
        if (existing == null) {
            return false;
        }
        userRoleMapper.deleteByUserId(userId);
        return removeById(userId);
    }

    private void validateUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        Long count = count(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
    }

    private String encodeIfNeeded(String password) {
        if (!StringUtils.hasText(password)) {
            return password;
        }
        boolean isBcrypt = password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
        return isBcrypt ? password : passwordEncoder.encode(password);
    }
}
