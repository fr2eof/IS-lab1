package se.ifmo.ru.back.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.ifmo.ru.back.service.CacheStatisticsService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * AOP аспект для логирования статистики L2 JPA Cache
 * Логирует cache hits и cache misses после выполнения методов репозиториев
 * Использует rate limiting для предотвращения избыточного логирования
 */
@Aspect
@Component
public class CacheStatisticsAspect {

    private static final Logger logger = LoggerFactory.getLogger(CacheStatisticsAspect.class);
    
    // Интервал между логированиями статистики (в миллисекундах) - 5 секунд
    private static final long LOG_INTERVAL_MS = 5000;

    private final CacheStatisticsService cacheStatisticsService;
    private final boolean statisticsEnabled;
    private final AtomicLong lastLogTime = new AtomicLong(0);

    public CacheStatisticsAspect(
            CacheStatisticsService cacheStatisticsService,
            @Value("${app.cache.stats.enabled:false}") boolean statisticsEnabled) {
        this.cacheStatisticsService = cacheStatisticsService;
        this.statisticsEnabled = statisticsEnabled;
    }

    /**
     * Перехватывает вызовы методов репозиториев и сервисов для логирования статистики кэша
     * Логирует статистику не чаще, чем раз в LOG_INTERVAL_MS миллисекунд
     */
    @Around("execution(* se.ifmo.ru.back.repository..*(..)) || " +
            "execution(* se.ifmo.ru.back.service..*(..))")
    public Object logCacheStatistics(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!statisticsEnabled) {
            // Если логирование отключено, просто выполняем метод
            return joinPoint.proceed();
        }

        // Выполняем метод
        Object result = joinPoint.proceed();

        // Логируем статистику с rate limiting (не чаще раза в LOG_INTERVAL_MS)
        long currentTime = System.currentTimeMillis();
        long lastTime = lastLogTime.get();
        
        if (currentTime - lastTime >= LOG_INTERVAL_MS) {
            if (lastLogTime.compareAndSet(lastTime, currentTime)) {
                try {
                    cacheStatisticsService.logCacheStatistics();
                } catch (Exception e) {
                    // Не прерываем выполнение, если логирование не удалось
                    logger.debug("Не удалось залогировать статистику кэша", e);
                }
            }
        }

        return result;
    }
}

