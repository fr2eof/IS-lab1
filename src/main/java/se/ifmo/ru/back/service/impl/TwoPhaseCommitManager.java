package se.ifmo.ru.back.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.ifmo.ru.back.service.S3StorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Менеджер двухфазного коммита для распределенной транзакции между БД и S3 хранилищем.
 * 
 * Реализует паттерн двухфазного коммита:
 * 1. Фаза подготовки (prepare): все участники готовятся к коммиту
 * 2. Фаза коммита (commit): все участники фиксируют изменения
 * 
 * Если на любой фазе происходит ошибка, выполняется откат (rollback) всех изменений.
 */
@Component
public class TwoPhaseCommitManager {

    private static final Logger logger = LoggerFactory.getLogger(TwoPhaseCommitManager.class);

    /**
     * Участник транзакции - представляет операцию, которую нужно выполнить
     */
    public static class TransactionParticipant {
        private final String name;
        private final Runnable prepareAction;
        private final Runnable commitAction;
        private final Runnable rollbackAction;
        private boolean prepared = false;
        private boolean committed = false;

        public TransactionParticipant(String name, Runnable prepareAction, Runnable commitAction, Runnable rollbackAction) {
            this.name = name;
            this.prepareAction = prepareAction;
            this.commitAction = commitAction;
            this.rollbackAction = rollbackAction;
        }

        public void prepare() {
            if (prepared) {
                logger.warn("Participant {} already prepared, skipping", name);
                return;
            }
            logger.info("=== PREPARE PHASE START ===");
            logger.info("Preparing participant: {}", name);
            try {
                prepareAction.run();
                prepared = true;
                logger.info("=== PREPARE PHASE SUCCESS ===");
                logger.info("Participant {} prepared successfully", name);
            } catch (Exception e) {
                logger.error("=== PREPARE PHASE FAILED ===");
                logger.error("Error preparing participant {}: {}", name, e.getMessage(), e);
                throw e;
            }
        }

        public void commit() {
            if (committed) {
                logger.warn("Participant {} already committed, skipping", name);
                return;
            }
            if (!prepared) {
                logger.error("Cannot commit participant {} - not prepared yet!", name);
                throw new IllegalStateException("Participant " + name + " must be prepared before commit");
            }
            logger.info("=== COMMIT PHASE START ===");
            logger.info("Committing participant: {}", name);
            try {
                commitAction.run();
                committed = true;
                logger.info("=== COMMIT PHASE SUCCESS ===");
                logger.info("Participant {} committed successfully", name);
            } catch (Exception e) {
                logger.error("=== COMMIT PHASE FAILED ===");
                logger.error("Error committing participant {}: {}", name, e.getMessage(), e);
                throw e;
            }
        }

        public void rollback() {
            if (!prepared) {
                logger.warn("Participant {} not prepared, nothing to rollback", name);
                return;
            }
            if (committed) {
                logger.warn("Participant {} already committed, cannot rollback", name);
                return;
            }
            logger.info("=== ROLLBACK PHASE START ===");
            logger.info("Rolling back participant: {}", name);
            try {
                rollbackAction.run();
                logger.info("=== ROLLBACK PHASE SUCCESS ===");
                logger.info("Participant {} rolled back successfully", name);
            } catch (Exception e) {
                logger.error("=== ROLLBACK PHASE FAILED ===");
                logger.error("Error rolling back participant {}: {}", name, e.getMessage(), e);
            }
        }
    }

    /**
     * Выполняет двухфазный коммит для списка участников
     * 
     * @param participants список участников транзакции
     * @throws Exception если произошла ошибка на любой фазе
     */
    public void executeTwoPhaseCommit(List<TransactionParticipant> participants) throws Exception {
        if (participants == null || participants.isEmpty()) {
            return;
        }

        logger.info("Starting two-phase commit with {} participants", participants.size());

        // ФАЗА 1: ПОДГОТОВКА (PREPARE)
        List<TransactionParticipant> preparedParticipants = new ArrayList<>();
        try {
            for (TransactionParticipant participant : participants) {
                try {
                    participant.prepare();
                    preparedParticipants.add(participant);
                } catch (Exception e) {
                    logger.error("Error preparing participant {}", participant.name, e);
                    // Откатываем всех подготовленных участников
                    rollbackAll(preparedParticipants);
                    throw new Exception("Failed to prepare transaction: " + e.getMessage(), e);
                }
            }
            logger.info("All participants prepared successfully");
        } catch (Exception e) {
            logger.error("Error in prepare phase", e);
            rollbackAll(preparedParticipants);
            throw e;
        }

        // ФАЗА 2: КОММИТ (COMMIT)
        List<TransactionParticipant> committedParticipants = new ArrayList<>();
        try {
            for (TransactionParticipant participant : preparedParticipants) {
                try {
                    participant.commit();
                    committedParticipants.add(participant);
                } catch (Exception e) {
                    logger.error("Error committing participant {}", participant.name, e);
                    // Откатываем всех закоммиченных участников
                    rollbackAll(committedParticipants);
                    throw new Exception("Failed to commit transaction: " + e.getMessage(), e);
                }
            }
            logger.info("All participants committed successfully");
        } catch (Exception e) {
            logger.error("Error in commit phase", e);
            rollbackAll(committedParticipants);
            throw e;
        }
    }

    /**
     * Откатывает всех участников транзакции
     */
    private void rollbackAll(List<TransactionParticipant> participants) {
        logger.info("Rolling back {} participants", participants.size());
        for (TransactionParticipant participant : participants) {
            try {
                participant.rollback();
            } catch (Exception e) {
                logger.error("Error rolling back participant {}", participant.name, e);
            }
        }
    }

    /**
     * Создает участника транзакции для S3 хранилища
     * 
     * @param s3StorageService сервис S3
     * @param fileContent содержимое файла
     * @param fileName имя файла
     * @return участник транзакции и путь к загруженному файлу (через массив)
     */
    public TransactionParticipant createS3Participant(
            S3StorageService s3StorageService,
            String fileContent,
            String fileName,
            String[] filePathHolder) {
        
        logger.info("=== CREATING S3 PARTICIPANT ===");
        logger.info("File name: {}", fileName);
        logger.info("File content length: {}", fileContent != null ? fileContent.length() : 0);
        logger.info("FilePathHolder array: {}", filePathHolder);
        
        return new TransactionParticipant(
                "S3 Storage",
                () -> {
                    // Подготовка: загружаем файл в S3
                    logger.info("=== S3 PARTICIPANT PREPARE ACTION START ===");
                    try {
                        logger.info("Calling s3StorageService.uploadFile()...");
                        filePathHolder[0] = s3StorageService.uploadFile(fileContent, fileName);
                        logger.info("=== S3 PARTICIPANT PREPARE ACTION SUCCESS ===");
                        logger.info("File prepared in S3. File path holder[0] = {}", filePathHolder[0]);
                        logger.info("FilePathHolder array after upload: {}", filePathHolder);
                    } catch (Exception e) {
                        logger.error("=== S3 PARTICIPANT PREPARE ACTION FAILED ===");
                        logger.error("Error preparing file in S3: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
                    }
                },
                () -> {
                    // Коммит: файл уже загружен, ничего не делаем
                    logger.info("=== S3 PARTICIPANT COMMIT ACTION START ===");
                    logger.info("S3 file committed. File path: {}", filePathHolder[0]);
                    logger.info("FilePathHolder array in commit: {}", filePathHolder);
                    logger.info("=== S3 PARTICIPANT COMMIT ACTION SUCCESS ===");
                },
                () -> {
                    // Откат: удаляем файл из S3
                    logger.info("=== S3 PARTICIPANT ROLLBACK ACTION START ===");
                    logger.info("FilePathHolder array in rollback: {}", filePathHolder);
                    if (filePathHolder[0] != null && !filePathHolder[0].isEmpty()) {
                        logger.info("Rolling back file: {}", filePathHolder[0]);
                        try {
                            s3StorageService.deleteFile(filePathHolder[0]);
                            logger.info("=== S3 PARTICIPANT ROLLBACK ACTION SUCCESS ===");
                            logger.info("S3 file rolled back: {}", filePathHolder[0]);
                        } catch (Exception e) {
                            logger.error("=== S3 PARTICIPANT ROLLBACK ACTION FAILED ===");
                            logger.error("Error rolling back file in S3: {}", e.getMessage(), e);
                            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
                        }
                    } else {
                        logger.warn("FilePathHolder[0] is null or empty, nothing to rollback");
                    }
                }
        );
    }

    /**
     * Создает участника транзакции для БД
     * 
     * @param dbAction действие для выполнения в БД (обычно сохранение в транзакции)
     * @return участник транзакции
     */
    public TransactionParticipant createDatabaseParticipant(Runnable dbAction) {
        return new TransactionParticipant(
                "Database",
                () -> {
                    // Подготовка: выполняем действие в БД (в рамках транзакции Spring)
                    // В Spring транзакции, подготовка происходит автоматически
                    logger.info("Database action prepared");
                },
                () -> {
                    // Коммит: выполняем действие в БД
                    // В Spring транзакции, коммит происходит автоматически при успешном завершении метода
                    dbAction.run();
                    logger.info("Database action committed");
                },
                () -> {
                    // Откат: Spring автоматически откатывает транзакцию при исключении
                    logger.info("Database action will be rolled back by Spring");
                }
        );
    }
}
