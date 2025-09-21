package se.ifmo.ru.lab1.service;

import se.ifmo.ru.lab1.entity.Chapter;

import java.util.List;
import java.util.Optional;

public interface ChapterService {
    
    Chapter createChapter(Chapter chapter);
    
    Optional<Chapter> getChapterById(Long id);
    
    List<Chapter> getAllChapters();
    
    Optional<Chapter> getChapterByName(String name);
    
    Chapter updateChapter(Long id, Chapter updatedChapter);
    
    boolean deleteChapter(Long id);
    
    boolean removeMarineFromChapter(Long chapterId);
}
