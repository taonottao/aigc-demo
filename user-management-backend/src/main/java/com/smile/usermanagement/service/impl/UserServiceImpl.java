package com.smile.usermanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.mapper.UserMapper;
import com.smile.usermanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

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
    public User createUser(User user) {
        validateUsernameUnique(user.getUsername(), null);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
        return user;
    }

    @Override
    public User updateUser(Long id, User user) {
        User existing = getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        validateUsernameUnique(user.getUsername(), id);

        existing.setUsername(user.getUsername());
        existing.setPassword(user.getPassword());
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setAvatar(user.getAvatar());
        existing.setOrgId(user.getOrgId());
        existing.setStatus(user.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        return existing;
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
}
