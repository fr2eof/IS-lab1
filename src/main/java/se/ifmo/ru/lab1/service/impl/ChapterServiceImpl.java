package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.ChapterDAO;
import se.ifmo.ru.lab1.dao.SpaceMarineDAO;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.entity.SpaceMarine;
import se.ifmo.ru.lab1.dto.RelatedObjectsResponse;
import se.ifmo.ru.lab1.dto.SpaceMarineDTO;
import se.ifmo.ru.lab1.service.ChapterService;
import se.ifmo.ru.lab1.mapper.SpaceMarineMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChapterServiceImpl implements ChapterService {

    @Inject
    private ChapterDAO chapterDAO;
    
    @Inject
    private SpaceMarineDAO spaceMarineDAO;
    
    @Inject
    private SpaceMarineMapper spaceMarineMapper;

    @Transactional
    public Chapter createChapter(Chapter chapter) {
        return chapterDAO.save(chapter);
    }

    public Optional<Chapter> getChapterById(Long id) {
        return chapterDAO.findById(id);
    }

    public List<Chapter> getAllChapters() {
        return chapterDAO.findAll();
    }

    public List<Chapter> getChapters(int page, int size) {
        return chapterDAO.findAll(page, size);
    }

    public List<Chapter> getChapters(int page, int size, String sortBy, String sortOrder) {
        return chapterDAO.findAll(page, size, sortBy, sortOrder);
    }

    public long getChaptersCount() {
        return chapterDAO.count();
    }

    public Optional<Chapter> getChapterByName(String name) {
        return chapterDAO.findByName(name);
    }

    @Transactional
    public Chapter updateChapter(Long id, Chapter updatedChapter) {
        Optional<Chapter> existingChapter = chapterDAO.findById(id);
        if (existingChapter.isPresent()) {
            Chapter chapter = existingChapter.get();
            chapter.setName(updatedChapter.getName());
            chapter.setMarinesCount(updatedChapter.getMarinesCount());
            return chapterDAO.update(chapter);
        }
        return null;
    }

    @Transactional
    public boolean deleteChapter(Long id) {
        Optional<Chapter> chapter = chapterDAO.findById(id);
        if (chapter.isPresent()) {
            // Автоматически удаляем всех маринов, связанных с этой главой
            long usageCount = spaceMarineDAO.countByChapterId(id);
            if (usageCount > 0) {
                spaceMarineDAO.deleteByChapterId(id);
            }
            
            chapterDAO.delete(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean removeMarineFromChapter(Long chapterId) {
        Optional<Chapter> chapter = chapterDAO.findById(chapterId);
        if (chapter.isPresent() && chapter.get().getMarinesCount() > 0) {
            chapterDAO.removeMarineFromChapter(chapterId);
            return true;
        }
        return false;
    }
    
    public RelatedObjectsResponse getRelatedObjects(Long id) {
        // Находим всех SpaceMarines, которые используют эту главу
        List<SpaceMarine> relatedMarines = spaceMarineDAO.findByChapterId(id);
        List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        return new RelatedObjectsResponse(relatedMarinesDTO);
    }
}
