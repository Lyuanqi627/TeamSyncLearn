package com.teamsync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.teamsync.mapper")
public class TeamsyncMvpApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamsyncMvpApplication.class, args);
    }
}
