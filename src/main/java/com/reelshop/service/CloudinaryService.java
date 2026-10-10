package com.reelshop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean    enabled;

    public CloudinaryService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}")    String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {

        this.enabled = !cloudName.isBlank() && !apiKey.isBlank();

        if (enabled) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
            ));
            log.info("Cloudinary enabled for cloud: {}", cloudName);
        } else {
            this.cloudinary = null;
            log.info("DEV MODE — Cloudinary not configured, using placeholder images");
        }
    }

    public UploadResult upload(MultipartFile file, String folder) throws IOException {
        if (!enabled) {
            log.info("DEV MODE — skipping image upload for: {}", file.getOriginalFilename());
            return new UploadResult(
                "https://placehold.co/400x400/1E1E2E/FF4D6D?text=" + folder,
                "dev_placeholder_" + System.currentTimeMillis()
            );
        }
        Map<?, ?> result = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap("folder", "reelshop/" + folder, "resource_type", "image")
        );
        return new UploadResult(
            (String) result.get("secure_url"),
            (String) result.get("public_id")
        );
    }

    public void delete(String publicId) {
        if (!enabled || publicId == null || publicId.startsWith("dev_")) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary image {}: {}", publicId, e.getMessage());
        }
    }

    public record UploadResult(String url, String publicId) {}
}
