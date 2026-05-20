package com.gate.mockexam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${gate.uploads.dir:./uploads/questions}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images at /uploads/questions/** from the local filesystem
        String absolutePath = Paths.get(uploadsDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/questions/**")
                .addResourceLocations(absolutePath);
    }
}
