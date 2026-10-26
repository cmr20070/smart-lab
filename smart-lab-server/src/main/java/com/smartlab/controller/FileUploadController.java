package com.smartlab.controller;

import com.smartlab.common.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    private Path uploadRoot;

    @PostConstruct
    public void init() {
        Path path = Paths.get(uploadPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(uploadPath);
        }
        this.uploadRoot = path;
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "avatars");
    }

    @PostMapping("/lab")
    public Result<Map<String, String>> uploadLabImage(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "labs");
    }

    @PostMapping("/device")
    public Result<Map<String, String>> uploadDeviceImage(@RequestParam("file") MultipartFile file) {
        return uploadImage(file, "devices");
    }

    private Result<Map<String, String>> uploadImage(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只允许上传图片文件");
        }

        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return Result.error(400, "图片大小不能超过2MB");
        }

        try {
            Path targetDir = uploadRoot.resolve(subDir);
            Files.createDirectories(targetDir);

            String filename = file.getOriginalFilename();
            if (filename == null || filename.isEmpty()) {
                filename = "image.jpg";
            }
            filename = filename.replace("\\", "/").replaceAll(".*/", "");

            Path filePath = targetDir.resolve(filename);
            file.transferTo(filePath.toFile());

            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + subDir + "/" + filename);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteImage(@RequestParam String url) {
        if (url == null || url.isEmpty()) {
            return Result.error(400, "文件路径不能为空");
        }
        try {
            String relativePath = url.replaceFirst("^/uploads/", "");
            Path filePath = uploadRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                return Result.error(403, "非法文件路径");
            }
            Files.deleteIfExists(filePath);
            Path parent = filePath.getParent();
            if (parent != null && Files.exists(parent) && !parent.equals(uploadRoot)) {
                try {
                    if (Files.list(parent).count() == 0) {
                        Files.delete(parent);
                    }
                } catch (IOException ignored) { }
            }
            return Result.success();
        } catch (NoSuchFileException e) {
            return Result.success();
        } catch (IOException e) {
            return Result.error(500, "文件删除失败: " + e.getMessage());
        }
    }
}