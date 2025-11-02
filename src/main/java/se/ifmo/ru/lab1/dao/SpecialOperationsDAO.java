package se.ifmo.ru.lab1.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class SpecialOperationsDAO {

    @PersistenceContext(unitName = "PostgresDS")
    private EntityManager entityManager;

    public BigDecimal getAverageHeartCount() {
        Query query = entityManager.createNativeQuery("SELECT get_average_heart_count()");
        return (BigDecimal) query.getSingleResult();
    }

    public Integer countMarinesByHealth(Integer healthThreshold) {
        Query query = entityManager.createNativeQuery("SELECT count_marines_by_health(?)");
        query.setParameter(1, healthThreshold);
        return ((Number) query.getSingleResult()).intValue();
    }

    public List<Object[]> findMarinesByNameContaining(String nameSubstring) {
        Query query = entityManager.createNativeQuery("SELECT * FROM find_marines_by_name(?)");
        query.setParameter(1, nameSubstring);
        return query.getResultList();
    }

    public Long createNewChapter(String chapterName, Integer marinesCount) {
        Query query = entityManager.createNativeQuery("SELECT create_new_chapter(?, ?)");
        query.setParameter(1, chapterName);
        query.setParameter(2, marinesCount);
        return ((Number) query.getSingleResult()).longValue();
    }

    public boolean removeMarineFromChapter(Long chapterId) {
        Query query = entityManager.createNativeQuery("SELECT remove_marine_from_chapter(?)");
        query.setParameter(1, chapterId);
        return (Boolean) query.getSingleResult();
    }
}
