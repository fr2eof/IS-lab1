package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.SpecialOperationsDAO;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.service.SpecialOperationsService;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class SpecialOperationsServiceImpl implements SpecialOperationsService {

    @Inject
    private SpecialOperationsDAO specialOperationsDAO;

    public Double getAverageHeartCount() {
        BigDecimal result = specialOperationsDAO.getAverageHeartCount();
        return result != null ? result.doubleValue() : 0.0;
    }

    public Integer countMarinesByHealth(Integer healthThreshold) {
        return specialOperationsDAO.countMarinesByHealth(healthThreshold);
    }

    public List<Object[]> findMarinesByNameContaining(String nameSubstring) {
        return specialOperationsDAO.findMarinesByNameContaining(nameSubstring);
    }

    @Transactional
    public Chapter createNewChapter(String chapterName, Integer marinesCount) {
        Long chapterId = specialOperationsDAO.createNewChapter(chapterName, marinesCount);
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setName(chapterName);
        chapter.setMarinesCount(marinesCount);
        return chapter;
    }

    @Transactional
    public boolean removeMarineFromChapter(Long chapterId) {
        return specialOperationsDAO.removeMarineFromChapter(chapterId);
    }
}
