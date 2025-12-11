package se.ifmo.ru.back.service;

import java.io.InputStream;

/**
 * Сервис для работы с S3-совместимым хранилищем (Yandex Object Storage)
 */
public interface S3StorageService {
    
    /**
     * Сохраняет файл в S3 хранилище
     * @param fileContent содержимое файла
     * @param fileName имя файла
     * @param importType тип импорта (spacemarines, coordinates, chapters)
     * @return путь к файлу в хранилище (ключ объекта)
     * @throws Exception если произошла ошибка при сохранении
     */
    String uploadFile(String fileContent, String fileName, String importType) throws Exception;
    
    /**
     * Сохраняет файл в S3 хранилище (из InputStream)
     * @param inputStream поток данных файла
     * @param fileName имя файла
     * @param contentLength размер файла
     * @return путь к файлу в хранилище (ключ объекта)
     * @throws Exception если произошла ошибка при сохранении
     */
    String uploadFile(InputStream inputStream, String fileName, long contentLength) throws Exception;
    
    /**
     * Удаляет файл из S3 хранилища
     * @param filePath путь к файлу (ключ объекта)
     * @throws Exception если произошла ошибка при удалении
     */
    void deleteFile(String filePath) throws Exception;
    
    /**
     * Получает содержимое файла из S3 хранилища
     * @param filePath путь к файлу (ключ объекта)
     * @return содержимое файла
     * @throws Exception если произошла ошибка при получении
     */
    byte[] downloadFile(String filePath) throws Exception;
    
    /**
     * Получает InputStream для файла из S3 хранилища
     * @param filePath путь к файлу (ключ объекта)
     * @return InputStream для чтения файла
     * @throws Exception если произошла ошибка при получении
     */
    InputStream downloadFileAsStream(String filePath) throws Exception;
    
    /**
     * Проверяет существование файла в S3 хранилище
     * @param filePath путь к файлу (ключ объекта)
     * @return true если файл существует, false иначе
     */
    boolean fileExists(String filePath);
    
    /**
     * Генерирует URL для скачивания файла (presigned URL)
     * @param filePath путь к файлу (ключ объекта)
     * @param expirationMinutes время жизни URL в минутах
     * @return URL для скачивания
     */
    String generateDownloadUrl(String filePath, int expirationMinutes) throws Exception;
    
    /**
     * Извлекает оригинальное имя файла из пути, убирая timestamp
     * @param filePath путь к файлу (ключ объекта)
     * @return оригинальное имя файла без timestamp
     */
    String extractOriginalFileName(String filePath);
}

