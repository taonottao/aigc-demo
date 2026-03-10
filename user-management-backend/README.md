# user-management-backend

Spring Boot 3 + JDK 21 + PostgreSQL + MyBatis + MyBatis-Plus backend.

## Run

```bash
mvn spring-boot:run
```

## API

- `GET /api/users?orgId=&keyword=`
- `GET /api/users/{id}`
- `POST /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

## Notes

- Database config follows local config values and uses database `user-management`.
- Initialize table SQL: `src/main/resources/schema.sql`.
