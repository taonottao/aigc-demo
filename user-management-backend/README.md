# user-management-backend

Spring Boot 3 + JDK 21 + PostgreSQL + MyBatis + MyBatis-Plus backend.

## Run

```bash
mvn spring-boot:run
```

## API

### Auth
- `GET /api/auth/captcha`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/second-verify`（敏感操作二次验证，返回 token）

### Users
- `GET /api/users?orgId=&keyword=`（需 `Authorization: Bearer <token>`）
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`（需 `X-Second-Verify: <token>`）
- `GET /api/users/export`（CSV）
- `POST /api/users/import`（CSV 文件）

### Orgs/Roles/Menus/Logs
- `GET /api/orgs/tree`
- `GET /api/roles`
- `GET /api/menus/tree`
- `GET /api/logs/login`
- `GET /api/logs/operations`

## Notes

- Database config follows local config values and uses database `user-management`.
- Initialize table SQL: `src/main/resources/schema.sql`（已包含组织/用户/角色/菜单/数据权限/日志表与基础数据）。
