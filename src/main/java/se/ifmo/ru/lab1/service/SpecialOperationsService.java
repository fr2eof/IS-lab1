package se.ifmo.ru.lab1.service;

import se.ifmo.ru.lab1.entity.Chapter;

import java.util.List;

public interface SpecialOperationsService {
    
    Double getAverageHeartCount();
    
    Integer countMarinesByHealth(Integer healthThreshold);
    
    List<Object[]> findMarinesByNameContaining(String nameSubstring);
    
    Chapter createNewChapter(String chapterName, Integer marinesCount);
    
    boolean removeMarineFromChapter(Long chapterId);
}
