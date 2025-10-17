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
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%"));
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
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%"));
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
}
