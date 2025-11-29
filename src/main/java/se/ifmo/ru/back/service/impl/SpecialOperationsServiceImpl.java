package se.ifmo.ru.back.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.repository.SpecialOperationsDAO;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.service.SpecialOperationsService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SpecialOperationsServiceImpl implements SpecialOperationsService {

    private final SpecialOperationsDAO specialOperationsDAO;

    public SpecialOperationsServiceImpl(SpecialOperationsDAO specialOperationsDAO) {
        this.specialOperationsDAO = specialOperationsDAO;
    }

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
