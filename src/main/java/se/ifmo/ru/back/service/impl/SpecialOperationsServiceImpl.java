package se.ifmo.ru.back.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.repository.SpecialOperationsDAO;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.service.SpecialOperationsService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SpecialOperationsServiceImpl implements SpecialOperationsService {

    private static final Logger logger = LoggerFactory.getLogger(SpecialOperationsServiceImpl.class);

    private final SpecialOperationsDAO specialOperationsDAO;

    public SpecialOperationsServiceImpl(SpecialOperationsDAO specialOperationsDAO) {
        this.specialOperationsDAO = specialOperationsDAO;
    }

    public Double getAverageHeartCount() {
        logger.info("Получение среднего значения heartCount");
        try {
            BigDecimal result = specialOperationsDAO.getAverageHeartCount();
            Double average = result != null ? result.doubleValue() : 0.0;
            logger.info("Среднее значение heartCount: {}", average);
            return average;
        } catch (Exception e) {
            logger.error("ОШИБКА при получении среднего значения heartCount", e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    public Integer countMarinesByHealth(Integer healthThreshold) {
        logger.info("Подсчет маринов по здоровью: healthThreshold={}", healthThreshold);
        try {
            Integer count = specialOperationsDAO.countMarinesByHealth(healthThreshold);
            logger.info("Найдено маринов с здоровьем меньше {}: {}", healthThreshold, count);
            return count;
        } catch (Exception e) {
            logger.error("ОШИБКА при подсчете маринов по здоровью: healthThreshold={}", healthThreshold, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    public List<Object[]> findMarinesByNameContaining(String nameSubstring) {
        logger.info("Поиск маринов по имени (содержит подстроку): nameSubstring={}", nameSubstring);
        try {
            List<Object[]> marines = specialOperationsDAO.findMarinesByNameContaining(nameSubstring);
            logger.info("Найдено маринов с именем, содержащим '{}': {}", nameSubstring, marines.size());
            return marines;
        } catch (Exception e) {
            logger.error("ОШИБКА при поиске маринов по имени: nameSubstring={}", nameSubstring, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    @Transactional
    public Chapter createNewChapter(String chapterName, Integer marinesCount) {
        logger.info("Создание новой главы через специальную операцию: name={}, marinesCount={}", 
            chapterName, marinesCount);
        try {
            logger.info("Вызов DAO для создания главы: name={}, marinesCount={}", chapterName, marinesCount);
            Long chapterId = specialOperationsDAO.createNewChapter(chapterName, marinesCount);
            logger.info("Глава создана с ID: {}", chapterId);
            
            Chapter chapter = new Chapter();
            chapter.setId(chapterId);
            chapter.setName(chapterName);
            chapter.setMarinesCount(marinesCount);
            
            logger.info("Глава успешно создана: id={}, name={}, marinesCount={}", 
                chapterId, chapterName, marinesCount);
            return chapter;
        } catch (Exception e) {
            logger.error("ОШИБКА при создании новой главы: name={}, marinesCount={}", 
                chapterName, marinesCount, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }

    @Transactional
    public boolean removeMarineFromChapter(Long chapterId) {
        logger.info("Удаление марина из главы через специальную операцию: chapterId={}", chapterId);
        try {
            boolean result = specialOperationsDAO.removeMarineFromChapter(chapterId);
            if (result) {
                logger.info("Марин успешно удален из главы: chapterId={}", chapterId);
            } else {
                logger.warn("Не удалось удалить марина из главы: chapterId={}", chapterId);
            }
            return result;
        } catch (Exception e) {
            logger.error("ОШИБКА при удалении марина из главы: chapterId={}", chapterId, e);
            logger.error("Тип ошибки: {}, сообщение: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Причина: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                logger.error("Stack trace причины:", e.getCause());
            }
            logger.error("Полный stack trace ошибки:", e);
            throw e;
        }
    }
}
