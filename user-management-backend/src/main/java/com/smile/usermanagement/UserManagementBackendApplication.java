package com.smile.usermanagement;

import com.smile.usermanagement.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.smile.usermanagement.mapper")
@EnableConfigurationProperties(JwtProperties.class)
public class UserManagementBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementBackendApplication.class, args);
    }
}
