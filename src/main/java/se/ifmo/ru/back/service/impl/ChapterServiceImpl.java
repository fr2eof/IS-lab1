package se.ifmo.ru.back.service.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.repository.ChapterRepository;
import se.ifmo.ru.back.repository.SpaceMarineRepository;
import se.ifmo.ru.back.dto.RelatedObjectsResponse;
import se.ifmo.ru.back.dto.SpaceMarineDTO;
import se.ifmo.ru.back.entity.Chapter;
import se.ifmo.ru.back.entity.SpaceMarine;
import se.ifmo.ru.back.exception.ValidationException;
import se.ifmo.ru.back.mapper.SpaceMarineMapper;
import se.ifmo.ru.back.service.ChapterService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final SpaceMarineRepository spaceMarineRepository;
    private final SpaceMarineMapper spaceMarineMapper;
    private final Validator validator;

    public ChapterServiceImpl(
            ChapterRepository chapterRepository,
            SpaceMarineRepository spaceMarineRepository,
            SpaceMarineMapper spaceMarineMapper,
            Validator validator) {
        this.chapterRepository = chapterRepository;
        this.spaceMarineRepository = spaceMarineRepository;
        this.spaceMarineMapper = spaceMarineMapper;
        this.validator = validator;
    }

    @Transactional
    public Chapter createChapter(Chapter chapter) {
        // Валидация сущности (включая кастомные валидаторы)
        Set<ConstraintViolation<Chapter>> violations = validator.validate(chapter);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ValidationException(errorMessage);
        }
        
        return chapterRepository.save(chapter);
    }

    public Optional<Chapter> getChapterById(Long id) {
        return chapterRepository.findById(id);
    }

    public List<Chapter> getAllChapters() {
        return chapterRepository.findAll();
    }

    public List<Chapter> getChapters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Chapter> pageResult = chapterRepository.findAll(pageable);
        return pageResult.getContent();
    }

    public List<Chapter> getChapters(int page, int size, String sortBy, String sortOrder) {
        Pageable pageable;
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(sortOrder) 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            pageable = PageRequest.of(page, size, sort);
        } else {
            pageable = PageRequest.of(page, size);
        }
        Page<Chapter> pageResult = chapterRepository.findAll(pageable);
        return pageResult.getContent();
    }

    public long getChaptersCount() {
        return chapterRepository.count();
    }

    public Optional<Chapter> getChapterByName(String name) {
        return chapterRepository.findByName(name);
    }

    @Transactional
    public Chapter updateChapter(Long id, Chapter updatedChapter) {
        Optional<Chapter> existingChapter = chapterRepository.findById(id);
        if (existingChapter.isPresent()) {
            Chapter chapter = existingChapter.get();
            chapter.setName(updatedChapter.getName());
            chapter.setMarinesCount(updatedChapter.getMarinesCount());
            
            // Валидация сущности (включая кастомные валидаторы)
            // ID уже установлен, поэтому валидатор исключит этот объект из проверки
            Set<ConstraintViolation<Chapter>> violations = validator.validate(chapter);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                throw new ValidationException(errorMessage);
            }
            
            return chapterRepository.save(chapter);
        }
        return null;
    }

    @Transactional
    public boolean deleteChapter(Long id) {
        Optional<Chapter> chapter = chapterRepository.findById(id);
        if (chapter.isPresent()) {
            // Автоматически удаляем всех маринов, связанных с этой главой
            long usageCount = spaceMarineRepository.countByChapterId(id);
            if (usageCount > 0) {
                spaceMarineRepository.deleteByChapterId(id);
            }
            
            chapterRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean removeMarineFromChapter(Long chapterId) {
        Optional<Chapter> chapter = chapterRepository.findById(chapterId);
        if (chapter.isPresent() && chapter.get().getMarinesCount() > 0) {
            int updated = chapterRepository.removeMarineFromChapter(chapterId);
            return updated > 0;
        }
        return false;
    }
    
    public RelatedObjectsResponse getRelatedObjects(Long id) {
        // Находим всех SpaceMarines, которые используют эту главу
        List<SpaceMarine> relatedMarines = spaceMarineRepository.findByChapterId(id);
        List<SpaceMarineDTO> relatedMarinesDTO = relatedMarines.stream()
                .map(spaceMarineMapper::toDTO)
                .collect(Collectors.toList());
        
        return new RelatedObjectsResponse(relatedMarinesDTO);
    }
}
