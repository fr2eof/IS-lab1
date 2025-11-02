package se.ifmo.ru.lab1.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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

    public List<Chapter> findAll(int page, int size) {
        TypedQuery<Chapter> query = entityManager.createQuery(
                "SELECT c FROM Chapter c", 
                Chapter.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<Chapter> findAll(int page, int size, String sortBy, String sortOrder) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Chapter> cq = cb.createQuery(Chapter.class);
        Root<Chapter> root = cq.from(Chapter.class);
        
        cq.select(root);
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.desc(root.get(sortBy)));
            } else {
                cq.orderBy(cb.asc(root.get(sortBy)));
            }
        }
        
        TypedQuery<Chapter> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM Chapter c", 
                Long.class);
        return query.getSingleResult();
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
