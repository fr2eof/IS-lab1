package se.ifmo.ru.back.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для управления состоянием логирования статистики L2 JPA Cache в рантайме
 * Изначальное состояние берется из properties, затем может изменяться через API
 */
@Service
public class CacheStatisticsStateService {

    private final AtomicBoolean statisticsEnabled;

    public CacheStatisticsStateService(
            @Value("${app.cache.stats.enabled:false}") boolean initialEnabled) {
        this.statisticsEnabled = new AtomicBoolean(initialEnabled);
    }

    /**
     * Проверяет, включено ли логирование статистики
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled.get();
    }

    /**
     * Включает логирование статистики
     */
    public void enableStatistics() {
        statisticsEnabled.set(true);
    }

    /**
     * Отключает логирование статистики
     */
    public void disableStatistics() {
        statisticsEnabled.set(false);
    }

    /**
     * Переключает состояние логирования статистики
     */
    public void toggleStatistics() {
        statisticsEnabled.set(!statisticsEnabled.get());
    }

    /**
     * Устанавливает состояние логирования статистики
     */
    public void setStatisticsEnabled(boolean enabled) {
        statisticsEnabled.set(enabled);
    }
}

