package com.erp.backend.service;

import com.erp.backend.exception.BusinessLogicException;
import com.erp.backend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.crm.upload-dir:uploads/crm}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Upload-Verzeichnis konnte nicht erstellt werden: " + uploadRoot, e);
        }
    }

    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessLogicException("Datei darf nicht leer sein");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessLogicException("Datei überschreitet die maximale Größe von 50 MB");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedName = UUID.randomUUID() + extension;
        Path targetDir = uploadRoot.resolve(subDirectory);

        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file: {} → {}", originalName, target);
            return subDirectory + "/" + storedName;
        } catch (IOException e) {
            throw new BusinessLogicException("Datei konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    public byte[] load(String relativePath) {
        Path filePath = uploadRoot.resolve(relativePath).normalize();

        if (!filePath.startsWith(uploadRoot)) {
            throw new BusinessLogicException("Ungültiger Dateipfad");
        }

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Datei nicht gefunden: " + relativePath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessLogicException("Datei konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        Path filePath = uploadRoot.resolve(relativePath).normalize();

        if (!filePath.startsWith(uploadRoot)) {
            throw new BusinessLogicException("Ungültiger Dateipfad");
        }

        try {
            Files.deleteIfExists(filePath);
            logger.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            logger.warn("Datei konnte nicht gelöscht werden: {}", filePath);
        }
    }

    public Path resolveAbsolutePath(String relativePath) {
        return uploadRoot.resolve(relativePath).normalize();
    }
}
