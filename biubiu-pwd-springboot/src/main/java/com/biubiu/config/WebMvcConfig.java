package com.biubiu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的静态资源映射
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String location = uploadDir.toString();
        
        // 确保路径以斜杠结尾
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location = location + "/";
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
