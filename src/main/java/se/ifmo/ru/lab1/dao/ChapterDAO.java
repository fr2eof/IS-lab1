package se.ifmo.ru.lab1.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import se.ifmo.ru.lab1.entity.Chapter;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ChapterDAO {

    @PersistenceContext(unitName = "PostgresDS")
    private EntityManager entityManager;

    public Chapter save(Chapter chapter) {
        entityManager.persist(chapter);
        return chapter;
    }

    public Optional<Chapter> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Chapter.class, id));
    }

    public List<Chapter> findAll() {
        TypedQuery<Chapter> query = entityManager.createQuery(
                "SELECT c FROM Chapter c", 
                Chapter.class);
        return query.getResultList();
    }

    public Optional<Chapter> findByName(String name) {
        TypedQuery<Chapter> query = entityManager.createQuery(
                "SELECT c FROM Chapter c WHERE c.name = :name", 
                Chapter.class);
        query.setParameter("name", name);
        List<Chapter> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Chapter update(Chapter chapter) {
        return entityManager.merge(chapter);
    }

    public void delete(Long id) {
        Chapter chapter = entityManager.find(Chapter.class, id);
        if (chapter != null) {
            entityManager.remove(chapter);
        }
    }

    public void removeMarineFromChapter(Long chapterId) {
        Chapter chapter = entityManager.find(Chapter.class, chapterId);
        if (chapter != null && chapter.getMarinesCount() > 0) {
            chapter.setMarinesCount(chapter.getMarinesCount() - 1);
            entityManager.merge(chapter);
        }
    }

    public void addMarineToChapter(Long chapterId) {
        Chapter chapter = entityManager.find(Chapter.class, chapterId);
        if (chapter != null && chapter.getMarinesCount() < 1000) {
            chapter.setMarinesCount(chapter.getMarinesCount() + 1);
            entityManager.merge(chapter);
        }
    }
}
