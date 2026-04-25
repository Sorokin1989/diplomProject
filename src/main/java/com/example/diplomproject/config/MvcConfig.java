//package com.example.diplomproject.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class MvcConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/uploads/categories/**")
//                .addResourceLocations("file:uploads/categories/");
//        registry.addResourceHandler("/uploads/courses/**")
//                .addResourceLocations("file:uploads/courses/");
//        registry.addResourceHandler("/uploads/materials/**")
//                .addResourceLocations("file:uploads/materials/");
//        registry.addResourceHandler("/uploads/certificates/**")
//                .addResourceLocations("file:uploads/certificates/");
//    }
//}

package com.example.diplomproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/categories/**")
                .addResourceLocations("file:" + uploadPath + "categories/");
        registry.addResourceHandler("/uploads/courses/**")
                .addResourceLocations("file:" + uploadPath + "courses/");
        registry.addResourceHandler("/uploads/materials/**")
                .addResourceLocations("file:" + uploadPath + "materials/");
        registry.addResourceHandler("/uploads/certificates/**")
                .addResourceLocations("file:" + uploadPath + "certificates/");
    }
}