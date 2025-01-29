package com.example.botfightwebserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@SpringBootApplication
@RestController
@EnableScheduling
public class BotFightWebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotFightWebServerApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer () {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("https://botfightrenderer-production.up.railway.app", "http://localhost:3000", "https://bytefight.org")
                    .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
