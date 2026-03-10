package com.smile.usermanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smile.usermanagement.mapper")
public class UserManagementBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementBackendApplication.class, args);
    }
}
