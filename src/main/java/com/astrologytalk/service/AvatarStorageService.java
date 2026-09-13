package com.astrologytalk.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {

  @Value("${app.uploads.dir:uploads}")
  private String uploadsDir;

  private static final Set<String> ALLOWED_MIME =
      Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

  private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

  public String store(MultipartFile file, Long userId) {
    if (file == null || file.isEmpty()) {
      throw new RuntimeException("File is empty");
    }
    if (file.getSize() > MAX_SIZE) {
      throw new RuntimeException("File too large (max 5MB)");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase())) {
      throw new RuntimeException("Only PNG, JPEG, or WEBP allowed");
    }

    try {
      Path avatarDir = Paths.get(uploadsDir, "avatars").toAbsolutePath();
      Files.createDirectories(avatarDir);

      String ext = getExtension(file.getOriginalFilename(), contentType);
      String filename = "user_" + userId + "_" + UUID.randomUUID() + ext;

      Path target = avatarDir.resolve(filename);
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

      // Public URL path served by WebConfig
      return "/uploads/avatars/" + filename;
    } catch (IOException e) {
      throw new RuntimeException("Failed to store file: " + e.getMessage());
    }
  }

  private String getExtension(String originalName, String contentType) {
    if (originalName != null && originalName.contains(".")) {
      String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
      if (ext.matches("\\.(png|jpg|jpeg|webp)")) return ext;
    }
    return switch (contentType.toLowerCase()) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
