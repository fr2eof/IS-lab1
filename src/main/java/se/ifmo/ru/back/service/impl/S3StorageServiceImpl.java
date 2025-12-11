package se.ifmo.ru.back.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import se.ifmo.ru.back.service.S3StorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3StorageServiceImpl implements S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public S3StorageServiceImpl(
            S3Client s3Client,
            @Value("${s3.bucket.name}") String bucketName,
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.region}") String region,
            @Value("${s3.access-key}") String accessKey,
            @Value("${s3.secret-key}") String secretKey) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        
        // Создаем bucket если его нет
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(headBucketRequest);
            logger.info("Bucket {} exists", bucketName);
        } catch (NoSuchBucketException e) {
            logger.info("Bucket {} does not exist, creating...", bucketName);
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.createBucket(createBucketRequest);
                logger.info("Bucket {} created successfully", bucketName);
            } catch (Exception ex) {
                logger.error("Failed to create bucket {}", bucketName, ex);
                throw new RuntimeException("Failed to create bucket: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            logger.error("Error checking bucket existence", e);
            // Не бросаем исключение, возможно bucket будет создан позже
        }
    }

    @Override
    public String uploadFile(String fileContent, String fileName, String importType) throws Exception {
        logger.info("=== S3 UPLOAD START ===");
        logger.info("File name: {}", fileName);
        logger.info("Import type: {}", importType);
        logger.info("File content length: {} characters", fileContent != null ? fileContent.length() : 0);
        logger.info("Bucket name: {}", bucketName);
        logger.info("Endpoint: {}", endpoint);
        logger.info("Region: {}", region);
        
        try {
            logger.info("Converting file content to bytes...");
            byte[] content = fileContent.getBytes("UTF-8");
            logger.info("File content converted to {} bytes", content.length);
            String result = uploadFile(new ByteArrayInputStream(content), fileName, content.length, importType);
            logger.info("=== S3 UPLOAD SUCCESS === File key: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("=== S3 UPLOAD FAILED ===", e);
            logger.error("Error details: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause: {}", e.getCause().getMessage());
            }
            throw new Exception("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName, long contentLength) throws Exception {
        // Для обратной совместимости используем "spacemarines" как тип по умолчанию
        return uploadFile(inputStream, fileName, contentLength, "spacemarines");
    }
    
    public String uploadFile(InputStream inputStream, String fileName, long contentLength, String importType) throws Exception {
        logger.info("=== S3 UPLOAD FILE (InputStream) START ===");
        logger.info("File name: {}, Content length: {} bytes, Import type: {}", fileName, contentLength, importType);
        
        try {
            // Генерируем уникальный ключ для файла
            logger.info("Generating file key...");
            String fileKey = generateFileKey(fileName, importType);
            logger.info("Generated file key: {}", fileKey);
            
            logger.info("Building PutObjectRequest...");
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType("application/json")
                    .build();
            logger.info("PutObjectRequest built: bucket={}, key={}", bucketName, fileKey);
            
            logger.info("Calling s3Client.putObject()...");
            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            logger.info("PutObjectResponse received: {}", response);
            logger.info("ETag: {}", response.eTag());
            logger.info("VersionId: {}", response.versionId());
            
            // Проверяем, что файл действительно загружен
            logger.info("Verifying file existence in S3...");
            boolean exists = fileExists(fileKey);
            logger.info("File exists check result: {}", exists);
            
            if (!exists) {
                logger.error("WARNING: File was uploaded but fileExists() returns false!");
            }
            
            logger.info("=== S3 UPLOAD FILE SUCCESS === File key: {}", fileKey);
            return fileKey;
        } catch (Exception e) {
            logger.error("=== S3 UPLOAD FILE FAILED ===", e);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            throw new Exception("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) throws Exception {
        logger.info("=== S3 DELETE FILE START ===");
        logger.info("File path: {}", filePath);
        logger.info("Bucket: {}", bucketName);
        
        try {
            logger.info("Checking if file exists before deletion...");
            boolean exists = fileExists(filePath);
            logger.info("File exists: {}", exists);
            
            if (!exists) {
                logger.warn("File {} does not exist in S3, skipping deletion", filePath);
                return;
            }
            
            logger.info("Building DeleteObjectRequest...");
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            logger.info("Calling s3Client.deleteObject()...");
            DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
            logger.info("DeleteObjectResponse: {}", response);
            logger.info("=== S3 DELETE FILE SUCCESS ===");
        } catch (Exception e) {
            logger.error("=== S3 DELETE FILE FAILED ===", e);
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            throw new Exception("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String filePath) throws Exception {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (NoSuchKeyException e) {
            logger.error("File {} not found in S3", filePath);
            throw new Exception("File not found in S3: " + filePath, e);
        } catch (Exception e) {
            logger.error("Error downloading file {} from S3", filePath, e);
            throw new Exception("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream downloadFileAsStream(String filePath) throws Exception {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            logger.error("File {} not found in S3", filePath);
            throw new Exception("File not found in S3: " + filePath, e);
        } catch (Exception e) {
            logger.error("Error downloading file {} from S3", filePath, e);
            throw new Exception("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        logger.debug("Checking file existence: bucket={}, key={}", bucketName, filePath);
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            logger.debug("File exists: bucket={}, key={}, size={}, lastModified={}", 
                bucketName, filePath, response.contentLength(), response.lastModified());
            return true;
        } catch (NoSuchKeyException e) {
            logger.debug("File does not exist: bucket={}, key={}", bucketName, filePath);
            return false;
        } catch (Exception e) {
            logger.error("Error checking file existence {} in S3: {}", filePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateDownloadUrl(String filePath, int expirationMinutes) throws Exception {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            try (S3Presigner presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build()) {
                
                return presigner.presignGetObject(presignRequest).url().toString();
            }
        } catch (Exception e) {
            logger.error("Error generating download URL for file {} from S3", filePath, e);
            throw new Exception("Failed to generate download URL: " + e.getMessage(), e);
        }
    }

    private String generateFileKey(String fileName, String importType) {
        // Генерируем уникальный ключ: imports/YYYY-MM-DD/importType/originalFileName
        // Структура: сначала дата, потом тип сущности
        String datePrefix = java.time.LocalDate.now().toString();
        String sanitizedFileName;
        
        if (fileName != null && !fileName.trim().isEmpty()) {
            // Используем оригинальное имя файла, очищенное от недопустимых символов
            sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            // Убираем расширение если есть, чтобы добавить его в конце
            String nameWithoutExt = sanitizedFileName;
            String extension = "";
            int lastDot = sanitizedFileName.lastIndexOf('.');
            if (lastDot > 0) {
                nameWithoutExt = sanitizedFileName.substring(0, lastDot);
                extension = sanitizedFileName.substring(lastDot);
            }
            // Добавляем timestamp для уникальности
            sanitizedFileName = nameWithoutExt + "_" + System.currentTimeMillis() + extension;
        } else {
            // Если имя не указано, генерируем UUID
            sanitizedFileName = UUID.randomUUID().toString() + ".json";
        }
        
        // Нормализуем тип импорта
        String normalizedType = importType != null ? importType.toLowerCase() : "spacemarines";
        if (!normalizedType.matches("^(spacemarines|coordinates|chapters)$")) {
            normalizedType = "spacemarines";
        }
        
        // Структура: imports/YYYY-MM-DD/importType/fileName
        return String.format("imports/%s/%s/%s", datePrefix, normalizedType, sanitizedFileName);
    }
    
    @Override
    public String extractOriginalFileName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "import.json";
        }
        
        // Извлекаем имя файла из пути
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        if (fileName.isEmpty()) {
            return "import.json";
        }
        
        // Убираем timestamp из имени файла
        // Формат: nameWithoutExt_timestamp.extension
        // Нужно вернуть: nameWithoutExt.extension
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            // Нет расширения, просто убираем timestamp если есть
            // Ищем последний _ и проверяем, является ли следующая часть числом (timestamp)
            int lastUnderscore = fileName.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String afterUnderscore = fileName.substring(lastUnderscore + 1);
                // Проверяем, является ли это числом (timestamp обычно 13 цифр)
                if (afterUnderscore.matches("^\\d+$") && afterUnderscore.length() >= 10) {
                    // Это timestamp, убираем его
                    return fileName.substring(0, lastUnderscore) + ".json";
                }
            }
            return fileName + ".json";
        }
        
        String extension = fileName.substring(lastDot);
        String nameWithTimestamp = fileName.substring(0, lastDot);
        
        // Ищем последний _ перед расширением
        int lastUnderscore = nameWithTimestamp.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String afterUnderscore = nameWithTimestamp.substring(lastUnderscore + 1);
            // Проверяем, является ли это числом (timestamp обычно 13 цифр)
            if (afterUnderscore.matches("^\\d+$") && afterUnderscore.length() >= 10) {
                // Это timestamp, убираем его
                return nameWithTimestamp.substring(0, lastUnderscore) + extension;
            }
        }
        
        // Если timestamp не найден, возвращаем как есть
        return fileName;
    }
}

