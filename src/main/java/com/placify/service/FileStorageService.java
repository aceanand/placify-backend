package com.placify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    public FileStorageResult storeResumeFile(MultipartFile file, Long userId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "resumes");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = String.format("resume_%d_%s_%s%s", 
            userId, timestamp, UUID.randomUUID().toString().substring(0, 8), fileExtension);
        
        // Store file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return FileStorageResult.builder()
            .fileName(uniqueFilename)
            .filePath(filePath.toString())
            .fileSize(file.getSize())
            .fileType(file.getContentType())
            .originalFileName(originalFilename)
            .build();
    }
    
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Failed to delete file: " + filePath + " - " + e.getMessage());
        }
    }
    
    public Resource loadFileAsResource(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("File not found or not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Invalid file path: " + filePath, e);
        }
    }
    
    public static class FileStorageResult {
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private String originalFileName;
        
        public static FileStorageResultBuilder builder() {
            return new FileStorageResultBuilder();
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public Long getFileSize() { return fileSize; }
        public String getFileType() { return fileType; }
        public String getOriginalFileName() { return originalFileName; }
        
        public static class FileStorageResultBuilder {
            private String fileName;
            private String filePath;
            private Long fileSize;
            private String fileType;
            private String originalFileName;
            
            public FileStorageResultBuilder fileName(String fileName) {
                this.fileName = fileName;
                return this;
            }
            
            public FileStorageResultBuilder filePath(String filePath) {
                this.filePath = filePath;
                return this;
            }
            
            public FileStorageResultBuilder fileSize(Long fileSize) {
                this.fileSize = fileSize;
                return this;
            }
            
            public FileStorageResultBuilder fileType(String fileType) {
                this.fileType = fileType;
                return this;
            }
            
            public FileStorageResultBuilder originalFileName(String originalFileName) {
                this.originalFileName = originalFileName;
                return this;
            }
            
            public FileStorageResult build() {
                FileStorageResult result = new FileStorageResult();
                result.fileName = this.fileName;
                result.filePath = this.filePath;
                result.fileSize = this.fileSize;
                result.fileType = this.fileType;
                result.originalFileName = this.originalFileName;
                return result;
            }
        }
    }
}