package se.ifmo.ru.back.service;

import lombok.Getter;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManagerFactory;


@Service
public class CacheStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(CacheStatisticsService.class);

    private final EntityManagerFactory entityManagerFactory;
    /**
     * -- GETTER --
     *  Проверяет, включено ли логирование статистики
     */
    @Getter
    private final boolean statisticsEnabled;

    public CacheStatisticsService(
            EntityManagerFactory entityManagerFactory,
            @Value("${app.cache.stats.enabled:false}") boolean statisticsEnabled) {
        this.entityManagerFactory = entityManagerFactory;
        this.statisticsEnabled = statisticsEnabled;
    }

    /**
     * Логирует статистику L2 JPA Cache (cache hits, cache misses)
     */
    public void logCacheStatistics() {
        if (!statisticsEnabled) {
            return;
        }

        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics statistics = sessionFactory.getStatistics();

            long secondLevelCacheHitCount = statistics.getSecondLevelCacheHitCount();
            long secondLevelCacheMissCount = statistics.getSecondLevelCacheMissCount();
            long secondLevelCachePutCount = statistics.getSecondLevelCachePutCount();
            long queryCacheHitCount = statistics.getQueryCacheHitCount();
            long queryCacheMissCount = statistics.getQueryCacheMissCount();
            long queryCachePutCount = statistics.getQueryCachePutCount();

            long totalCacheHits = secondLevelCacheHitCount + queryCacheHitCount;
            long totalCacheMisses = secondLevelCacheMissCount + queryCacheMissCount;
            long totalCacheRequests = totalCacheHits + totalCacheMisses;

            double hitRatio = totalCacheRequests > 0
                    ? (double) totalCacheHits / totalCacheRequests * 100.0
                    : 0.0;

            logger.info("=== L2 JPA Cache Statistics ===");
            logger.info("Second Level Cache - Hits: {}, Misses: {}, Puts: {}",
                    secondLevelCacheHitCount, secondLevelCacheMissCount, secondLevelCachePutCount);
            logger.info("Query Cache - Hits: {}, Misses: {}, Puts: {}",
                    queryCacheHitCount, queryCacheMissCount, queryCachePutCount);
            logger.info("Total Cache - Hits: {}, Misses: {}, Requests: {}, Hit Ratio: {}%",
                    totalCacheHits, totalCacheMisses, totalCacheRequests, hitRatio);
            logger.info("================================");
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики кэша", e);
        }
    }

}
