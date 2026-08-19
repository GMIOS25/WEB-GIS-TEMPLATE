package com.website.gis.core.storage;

import com.website.gis.core.exception.BadRequestException;
import com.website.gis.core.exception.ResourceNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024; // 20 MB

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.local-path:./data/uploads}") String uploadPath) {
        this.rootLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory: " + uploadPath, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Cannot store file with relative path outside current directory: " + originalFilename);
        }

        String detectedMimeType = detectAndValidateMimeType(file, originalFilename);

        Path targetDir = this.rootLocation;
        String cleanSubDir = "";
        if (StringUtils.hasText(subDirectory)) {
            String sanitizedSubDir = StringUtils.cleanPath(subDirectory).replace("\\", "/");
            if (sanitizedSubDir.contains("..") || sanitizedSubDir.startsWith("/")) {
                throw new BadRequestException("Invalid sub-directory: " + subDirectory);
            }
            cleanSubDir = sanitizedSubDir + "/";
            targetDir = this.rootLocation.resolve(sanitizedSubDir).normalize();
            try {
                Files.createDirectories(targetDir);
            } catch (IOException e) {
                throw new IllegalStateException("Could not create sub-directory: " + targetDir, e);
            }
        }

        String fileExtension = getExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        Path targetFile = targetDir.resolve(uniqueFileName).normalize();

        try {
            if (isImageMimeType(detectedMimeType)) {
                // Resize image to max 1600px width/height while maintaining aspect ratio and quality
                try (InputStream inputStream = file.getInputStream()) {
                    Thumbnails.of(inputStream)
                            .size(1600, 1600)
                            .outputQuality(0.85)
                            .toFile(targetFile.toFile());
                }
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to store file {}", uniqueFileName, e);
            throw new IllegalStateException("Failed to store file " + originalFilename, e);
        }

        long storedSizeBytes;
        try {
            storedSizeBytes = Files.size(targetFile);
        } catch (IOException e) {
            storedSizeBytes = file.getSize();
        }

        String storedRelativePath = cleanSubDir + uniqueFileName;
        String publicUrl = "/api/files/" + storedRelativePath;

        return new StoredFile(
                storedRelativePath,
                originalFilename,
                detectedMimeType,
                storedSizeBytes,
                publicUrl
        );
    }

    @Override
    public Resource loadAsResource(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            throw new ResourceNotFoundException("File name cannot be empty");
        }

        try {
            String cleanName = StringUtils.cleanPath(storedFileName);
            if (cleanName.contains("..")) {
                throw new BadRequestException("Invalid file path: " + storedFileName);
            }

            Path file = this.rootLocation.resolve(cleanName).normalize();
            if (!file.startsWith(this.rootLocation)) {
                throw new BadRequestException("Access denied to path: " + storedFileName);
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + storedFileName);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + storedFileName);
        }
    }

    @Override
    public void delete(String storedFileName) {
        if (!StringUtils.hasText(storedFileName)) {
            return;
        }

        try {
            String cleanName = StringUtils.cleanPath(storedFileName);
            if (cleanName.contains("..")) {
                return;
            }
            Path file = this.rootLocation.resolve(cleanName).normalize();
            if (file.startsWith(this.rootLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            logger.warn("Could not delete file {}", storedFileName, e);
        }
    }

    private String detectAndValidateMimeType(MultipartFile file, String filename) {
        String ext = getExtension(filename).toLowerCase(Locale.ROOT);
        byte[] header = new byte[8];
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            int read = is.read(header, 0, header.length);
            if (read < 4) {
                throw new BadRequestException("Corrupted or unreadable file content");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not inspect file header", e);
        }

        // 1. JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            if (!Arrays.asList("jpg", "jpeg").contains(ext)) {
                throw new BadRequestException("File extension does not match JPEG content");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("Image file size exceeds maximum limit of 5MB");
            }
            return "image/jpeg";
        }

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            if (!"png".equals(ext)) {
                throw new BadRequestException("File extension does not match PNG content");
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("Image file size exceeds maximum limit of 5MB");
            }
            return "image/png";
        }

        // 3. PDF: %PDF (25 50 44 46)
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
            if (!"pdf".equals(ext)) {
                throw new BadRequestException("File extension does not match PDF content");
            }
            if (file.getSize() > MAX_DOCUMENT_SIZE) {
                throw new BadRequestException("Document file size exceeds maximum limit of 20MB");
            }
            return "application/pdf";
        }

        // 4. DOCX / ZIP: PK.. (50 4B 03 04)
        if (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04) {
            if (!"docx".equals(ext)) {
                throw new BadRequestException("Only .docx Word documents are supported for ZIP-based files");
            }
            if (file.getSize() > MAX_DOCUMENT_SIZE) {
                throw new BadRequestException("Document file size exceeds maximum limit of 20MB");
            }
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        throw new BadRequestException("Unsupported or spoofed file format. Allowed formats: JPEG, PNG, PDF, DOCX");
    }

    private boolean isImageMimeType(String mimeType) {
        return "image/jpeg".equals(mimeType) || "image/png".equals(mimeType);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}
