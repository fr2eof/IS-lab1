package se.ifmo.ru.lab1.service.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.ifmo.ru.lab1.dao.ChapterDAO;
import se.ifmo.ru.lab1.entity.Chapter;
import se.ifmo.ru.lab1.service.ChapterService;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterServiceImpl implements ChapterService {

    @Inject
    private ChapterDAO chapterDAO;

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
}
