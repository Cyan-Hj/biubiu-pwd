package com.biubiu.controller;

import com.biubiu.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Map<byte[], String> MAGIC_BYTES = new LinkedHashMap<>();

    static {
        MAGIC_BYTES.put(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "image/jpeg");
        MAGIC_BYTES.put(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png");
        MAGIC_BYTES.put(new byte[]{0x47, 0x49, 0x46, 0x38}, "image/gif");
        MAGIC_BYTES.put(new byte[]{0x52, 0x49, 0x46, 0x46}, "image/webp");
        MAGIC_BYTES.put(new byte[]{0x42, 0x4D}, "image/bmp");
    }

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @PostMapping
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("请选择文件");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";

        String contentType = file.getContentType();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("不支持的文件扩展名: {}", extension);
            return ApiResponse.error("不支持的文件扩展名: " + extension);
        }

        if (contentType != null && !contentType.startsWith("image/")) {
            log.warn("不支持的Content-Type: {}", contentType);
            return ApiResponse.error("不支持的Content-Type: " + contentType);
        }

        try {
            byte[] fileBytes = file.getBytes();
            String detectedType = detectImageType(fileBytes);

            if (detectedType == null) {
                if (extension.equals(".png")) {
                    detectedType = "image/png";
                } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                    detectedType = "image/jpeg";
                } else if (extension.equals(".bmp")) {
                    detectedType = "image/bmp";
                } else {
                    log.warn("无法识别图片格式，文件头: {}", bytesToHex(fileBytes, 16));
                    return ApiResponse.error("无法识别图片格式");
                }
            }

            if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
                log.warn("检测到的图片类型不在允许列表中: {}", detectedType);
                return ApiResponse.error("不支持的图片类型: " + detectedType);
            }

            Path uploadDir;
            if (Paths.get(uploadPath).isAbsolute()) {
                uploadDir = Paths.get(uploadPath);
            } else {
                String userDir = System.getProperty("user.dir");
                uploadDir = Paths.get(userDir, uploadPath);
            }

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String newFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(newFilename);

            Files.write(filePath, fileBytes);

            String fileUrl = "/uploads/" + newFilename;
            log.info("文件上传成功: {}", fileUrl);
            return ApiResponse.success("上传成功", fileUrl);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes, int length) {
        if (bytes == null || bytes.length == 0) {
            return "empty";
        }
        int len = Math.min(length, bytes.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    private String detectImageType(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 4) {
            return null;
        }

        for (Map.Entry<byte[], String> entry : MAGIC_BYTES.entrySet()) {
            byte[] magic = entry.getKey();
            if (fileBytes.length >= magic.length) {
                boolean match = true;
                for (int i = 0; i < magic.length; i++) {
                    if (fileBytes[i] != magic[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
