package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @PostMapping
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("收到上传请求，文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            log.warn("文件为空");
            return ApiResponse.error("请选择文件");
        }

        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            log.info("上传目录: {}", uploadDir.toAbsolutePath());
            
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("创建上传目录成功");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);
            log.info("文件保存成功: {}", filePath.toAbsolutePath());

            // 返回文件访问URL
            String fileUrl = "/uploads/" + newFilename;
            return ApiResponse.success("上传成功", fileUrl);

        } catch (IOException e) {
            log.error("上传失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }
}
