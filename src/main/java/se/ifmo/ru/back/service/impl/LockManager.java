package se.ifmo.ru.back.service.impl;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Менеджер блокировок на уровне приложения для предотвращения race conditions
 * при конкурентном импорте и создании сущностей.
 * 
 * Использует ReentrantLock для синхронизации доступа к критическим секциям
 * проверки уникальности.
 */
@Component
public class LockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Получить блокировку для указанного ключа.
     * Если блокировка не существует, создается новая.
     * 
     * @param key ключ блокировки (например, "coords_10.5_20.3")
     * @return блокировка для указанного ключа
     */
    public ReentrantLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * Заблокировать и выполнить действие, затем автоматически разблокировать.
     * Гарантирует, что блокировка будет освобождена даже при исключении.
     * 
     * @param key ключ блокировки
     * @param action действие для выполнения под блокировкой
     * @return результат выполнения действия
     */
    public <T> T executeWithLock(String key, java.util.function.Supplier<T> action) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Заблокировать и выполнить действие без возвращаемого значения.
     * 
     * @param key ключ блокировки
     * @param action действие для выполнения под блокировкой
     */
    public void executeWithLock(String key, Runnable action) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Очистить неиспользуемые блокировки (опционально, для экономии памяти).
     * Можно вызывать периодически, но обычно не требуется, так как
     * ConcurrentHashMap эффективно управляет памятью.
     */
    public void cleanup() {
        // Удаляем блокировки, которые не заблокированы и не используются
        locks.entrySet().removeIf(entry -> 
            !entry.getValue().isLocked() && entry.getValue().getQueueLength() == 0
        );
    }
}


