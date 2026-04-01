package com.example.diplomproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Внешняя папка uploads в корне проекта (или в абсолютном пути)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}