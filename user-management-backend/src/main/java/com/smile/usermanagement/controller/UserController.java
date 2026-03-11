package com.smile.usermanagement.controller;

import com.smile.usermanagement.dto.UserCreateRequest;
import com.smile.usermanagement.dto.UserImportResult;
import com.smile.usermanagement.dto.UserResponse;
import com.smile.usermanagement.dto.UserUpdateRequest;
import com.smile.usermanagement.entity.User;
import com.smile.usermanagement.service.UserService;
import jakarta.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestParam(required = false) Long orgId,
                                        @RequestParam(required = false) String keyword) {
        List<User> users = userService.listByOrgAndKeyword(orgId, keyword);
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<Long>> roleMap = userService.getRoleIdsMap(userIds);
        List<UserResponse> responses = new ArrayList<>();
        for (User user : users) {
            responses.add(toResponse(user, roleMap.getOrDefault(user.getId(), Collections.emptyList())));
        }
        return responses;
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        List<Long> roleIds = userService.getRoleIds(user.getId());
        return toResponse(user, roleIds);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        User created = userService.createUser(user, request.getRoleIds());
        List<Long> roleIds = userService.getRoleIds(created.getId());
        return toResponse(created, roleIds);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User user = new User();
        fillUser(user, request.getUsername(), request.getPassword(), request.getRealName(),
            request.getPhone(), request.getEmail(), request.getAvatar(), request.getOrgId(), request.getStatus());
        User updated = userService.updateUser(id, user, request.getRoleIds());
        List<Long> roleIds = userService.getRoleIds(updated.getId());
        return toResponse(updated, roleIds);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        boolean removed = userService.removeById(id);
        if (!removed) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserImportResult importUsers(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) Long defaultOrgId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Import file is empty");
        }
        UserImportResult result = new UserImportResult();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (!org.springframework.util.StringUtils.hasText(line)) {
                    continue;
                }
                if (row == 1 && line.toLowerCase().contains("username")) {
                    continue;
                }
                result.setTotal(result.getTotal() + 1);
                try {
                    ImportRow parsed = parseImportLine(line, defaultOrgId);
                    User user = new User();
                    fillUser(user, parsed.username, parsed.password, parsed.realName,
                        parsed.phone, parsed.email, parsed.avatar, parsed.orgId, parsed.status);
                    userService.createUser(user, parsed.roleIds);
                    result.setSuccess(result.getSuccess() + 1);
                } catch (Exception ex) {
                    result.setFailed(result.getFailed() + 1);
                    result.addError("Row " + row + ": " + ex.getMessage());
                }
            }
        }
        return result;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportUsers(@RequestParam(required = false) Long orgId,
                                              @RequestParam(required = false) String keyword) {
        List<User> users = userService.listByOrgAndKeyword(orgId, keyword);
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<Long>> roleMap = userService.getRoleIdsMap(userIds);
        StringBuilder csv = new StringBuilder();
        csv.append("username,password,real_name,phone,email,avatar,org_id,status,role_ids,created_at,updated_at\n");
        for (User user : users) {
            List<Long> roleIds = roleMap.getOrDefault(user.getId(), Collections.emptyList());
            csv.append(escapeCsv(user.getUsername())).append(',')
                .append("").append(',')
                .append(escapeCsv(user.getRealName())).append(',')
                .append(escapeCsv(user.getPhone())).append(',')
                .append(escapeCsv(user.getEmail())).append(',')
                .append(escapeCsv(user.getAvatar())).append(',')
                .append(user.getOrgId() == null ? "" : user.getOrgId()).append(',')
                .append(user.getStatus() == null ? "" : user.getStatus()).append(',')
                .append(escapeCsv(joinRoleIds(roleIds))).append(',')
                .append(formatTime(user.getCreatedAt())).append(',')
                .append(formatTime(user.getUpdatedAt()))
                .append("\n");
        }
        String filename = "users_export.csv";
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.toString().getBytes(StandardCharsets.UTF_8));
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

    private UserResponse toResponse(User user, List<Long> roleIds) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setOrgId(user.getOrgId());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRoleIds(roleIds);
        return response;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String joinRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("|");
        for (Long roleId : roleIds) {
            joiner.add(String.valueOf(roleId));
        }
        return joiner.toString();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.toString();
    }

    private ImportRow parseImportLine(String line, Long defaultOrgId) {
        String[] parts = splitCsvLine(line);
        if (parts.length < 7) {
            throw new IllegalArgumentException("Not enough columns");
        }
        ImportRow row = new ImportRow();
        row.username = parts[0].trim();
        row.password = parts[1].trim();
        row.realName = parts[2].trim();
        row.phone = parts[3].trim();
        row.email = parts[4].trim();
        row.avatar = parts[5].trim();
        row.orgId = parseLongOrDefault(parts[6], defaultOrgId);
        if (row.orgId == null) {
            throw new IllegalArgumentException("org_id is required");
        }
        row.status = parts.length > 7 && org.springframework.util.StringUtils.hasText(parts[7])
            ? Integer.parseInt(parts[7].trim()) : 1;
        row.roleIds = parts.length > 8 ? parseRoleIds(parts[8]) : Collections.emptyList();
        if (!org.springframework.util.StringUtils.hasText(row.username)) {
            throw new IllegalArgumentException("username is required");
        }
        if (!org.springframework.util.StringUtils.hasText(row.password)) {
            throw new IllegalArgumentException("password is required");
        }
        if (!org.springframework.util.StringUtils.hasText(row.realName)) {
            throw new IllegalArgumentException("real_name is required");
        }
        return row;
    }

    private String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    private List<Long> parseRoleIds(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split("[,|;]");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            if (org.springframework.util.StringUtils.hasText(part)) {
                ids.add(Long.parseLong(part.trim()));
            }
        }
        return ids;
    }

    private static class ImportRow {
        private String username;
        private String password;
        private String realName;
        private String phone;
        private String email;
        private String avatar;
        private Long orgId;
        private Integer status;
        private List<Long> roleIds;
    }
}
