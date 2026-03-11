package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.Role;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.entity.UserRole;
import com.smile.usermanagement.mapper.RoleMapper;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.mapper.UserRoleMapper;
import com.smile.usermanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRoleMapper userRoleMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> listByOrgAndKeyword(Long orgId, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(User::getOrgId, orgId);
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
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
        user.setPassword(encodePasswordIfNeeded(user.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        replaceRoles(user.getId(), roleIds);
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
            existing.setPassword(encodePasswordIfNeeded(user.getPassword()));
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
            replaceRoles(existing.getId(), roleIds);
        }
        return existing;
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        return roleIds == null ? Collections.emptyList() : roleIds;
    }

    @Override
    public Map<Long, List<Long>> getRoleIdsMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<UserRole> relations = userRoleMapper.selectByUserIds(userIds);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> result = new HashMap<>();
        for (UserRole relation : relations) {
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>())
                .add(relation.getRoleId());
        }
        return result;
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

    private void replaceRoles(Long userId, List<Long> roleIds) {
        List<Long> normalized = normalizeRoleIds(roleIds);
        if (!CollectionUtils.isEmpty(normalized)) {
            validateRoleIds(normalized);
        }
        userRoleMapper.deleteByUserId(userId);
        if (CollectionUtils.isEmpty(normalized)) {
            return;
        }
        List<UserRole> rows = new ArrayList<>();
        for (Long roleId : normalized) {
            UserRole row = new UserRole();
            row.setUserId(userId);
            row.setRoleId(roleId);
            rows.add(row);
        }
        saveUserRoles(rows);
    }

    private void saveUserRoles(List<UserRole> rows) {
        for (UserRole row : rows) {
            userRoleMapper.insert(row);
        }
    }

    private void validateRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.in("id", roleIds);
        Long count = roleMapper.selectCount(wrapper);
        if (count == null || count != roleIds.size()) {
            throw new IllegalArgumentException("Role id list contains invalid value");
        }
    }

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> set = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            if (roleId != null) {
                set.add(roleId);
            }
        }
        return new ArrayList<>(set);
    }

    private String encodePasswordIfNeeded(String password) {
        if (!StringUtils.hasText(password)) {
            return password;
        }
        if (isBcryptHash(password)) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
