package com.smile.usermanagement.controller;

import com.smile.usermanagement.dto.UserCreateRequest;
import com.smile.usermanagement.dto.UserUpdateRequest;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> listUsers(@RequestParam(required = false) Long orgId,
                                @RequestParam(required = false) String keyword) {
        return userService.listByOrgAndKeyword(orgId, keyword);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        boolean removed = userService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    private void fillUser(User user, String username, String password, String realName,
                          String phone, String email, String avatar, Long orgId, Integer status) {
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAvatar(avatar);
        user.setOrgId(orgId);
        user.setStatus(status);
    }
}
