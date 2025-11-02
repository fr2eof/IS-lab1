package se.ifmo.ru.lab1.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import se.ifmo.ru.lab1.entity.SpaceMarine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SpaceMarineDAO {

    @PersistenceContext(unitName = "PostgresDS")
    private EntityManager entityManager;

    public SpaceMarine save(SpaceMarine spaceMarine) {
        entityManager.persist(spaceMarine);
        return spaceMarine;
    }

    public Optional<SpaceMarine> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(SpaceMarine.class, id));
    }

    public List<SpaceMarine> findAll() {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter", 
                SpaceMarine.class);
        return query.getResultList();
    }

    public List<SpaceMarine> findAll(int page, int size) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter", 
                SpaceMarine.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<SpaceMarine> findAll(int page, int size, String sortBy, String sortOrder) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SpaceMarine> cq = cb.createQuery(SpaceMarine.class);
        Root<SpaceMarine> root = cq.from(SpaceMarine.class);
        
        cq.select(root);
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            try {
                if ("desc".equalsIgnoreCase(sortOrder)) {
                    cq.orderBy(cb.desc(root.get(sortBy)));
                } else {
                    cq.orderBy(cb.asc(root.get(sortBy)));
                }
            } catch (Exception e) {
                // Если поле не найдено, игнорируем сортировку
            }
        }
        
        TypedQuery<SpaceMarine> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList();
    }

    public List<SpaceMarine> findByNameContaining(String name) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter " +
                "WHERE sm.name LIKE :name", 
                SpaceMarine.class);
        query.setParameter("name", "%" + name + "%");
        return query.getResultList();
    }

    public List<SpaceMarine> findByHealthLessThan(Integer health) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter " +
                "WHERE sm.health < :health", 
                SpaceMarine.class);
        query.setParameter("health", health);
        return query.getResultList();
    }

    public long countByHealthLessThan(Integer health) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(sm) FROM SpaceMarine sm WHERE sm.health < :health", 
                Long.class);
        query.setParameter("health", health);
        return query.getSingleResult();
    }

    public Double getAverageHeartCount() {
        TypedQuery<Double> query = entityManager.createQuery(
                "SELECT AVG(sm.heartCount) FROM SpaceMarine sm", 
                Double.class);
        return query.getSingleResult();
    }

    public List<SpaceMarine> findWithFilters(String nameFilter, String sortBy, String sortOrder, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SpaceMarine> cq = cb.createQuery(SpaceMarine.class);
        Root<SpaceMarine> root = cq.from(SpaceMarine.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(root.get("name")), nameFilter.toLowerCase().trim()));
        }
        
        cq.where(predicates.toArray(new Predicate[0]));
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.desc(root.get(sortBy)));
            } else {
                cq.orderBy(cb.asc(root.get(sortBy)));
            }
        }
        
        TypedQuery<SpaceMarine> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList();
    }

    public long countWithFilters(String nameFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<SpaceMarine> root = cq.from(SpaceMarine.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            predicates.add(cb.equal(cb.lower(root.get("name")), nameFilter.toLowerCase().trim()));
        }
        
        cq.select(cb.count(root));
        cq.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(cq).getSingleResult();
    }

    public SpaceMarine update(SpaceMarine spaceMarine) {
        return entityManager.merge(spaceMarine);
    }

    public void delete(Integer id) {
        SpaceMarine spaceMarine = entityManager.find(SpaceMarine.class, id);
        if (spaceMarine != null) {
            entityManager.remove(spaceMarine);
        }
    }

    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(sm) FROM SpaceMarine sm", 
                Long.class);
        return query.getSingleResult();
    }

    public List<SpaceMarine> findByCoordinatesId(Long coordinatesId) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId", 
                SpaceMarine.class);
        query.setParameter("coordinatesId", coordinatesId);
        return query.getResultList();
    }

    public List<SpaceMarine> findByChapterId(Long chapterId) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId", 
                SpaceMarine.class);
        query.setParameter("chapterId", chapterId);
        return query.getResultList();
    }

    public long countByCoordinatesId(Long coordinatesId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(sm) FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId", 
                Long.class);
        query.setParameter("coordinatesId", coordinatesId);
        return query.getSingleResult();
    }

    public long countByChapterId(Long chapterId) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(sm) FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId", 
                Long.class);
        query.setParameter("chapterId", chapterId);
        return query.getSingleResult();
    }

    public void deleteByChapterId(Long chapterId) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId", 
                SpaceMarine.class);
        query.setParameter("chapterId", chapterId);
        List<SpaceMarine> marines = query.getResultList();
        
        for (SpaceMarine marine : marines) {
            entityManager.remove(marine);
        }
    }

    public void deleteByCoordinatesId(Long coordinatesId) {
        TypedQuery<SpaceMarine> query = entityManager.createQuery(
                "SELECT sm FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId", 
                SpaceMarine.class);
        query.setParameter("coordinatesId", coordinatesId);
        List<SpaceMarine> marines = query.getResultList();
        
        for (SpaceMarine marine : marines) {
            entityManager.remove(marine);
        }
    }
}
