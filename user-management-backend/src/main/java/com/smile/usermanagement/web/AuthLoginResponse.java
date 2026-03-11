package com.smile.usermanagement.web;

import com.smile.usermanagement.entity.Menu;
import com.smile.usermanagement.entity.User;
import java.util.List;

public record AuthLoginResponse(
    String token,
    UserInfo user,
    List<Menu> menus,
    List<String> permissions
) {
    public record UserInfo(
        Long id,
        String username,
        String realName,
        Long orgId
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getRealName(), user.getOrgId());
        }
    }
}

