package se.ifmo.ru.lab1.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import se.ifmo.ru.lab1.entity.Coordinates;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CoordinatesDAO {

    @PersistenceContext(unitName = "PostgresDS")
    private EntityManager entityManager;

    public Coordinates save(Coordinates coordinates) {
        entityManager.persist(coordinates);
        return coordinates;
    }

    public Optional<Coordinates> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Coordinates.class, id));
    }

    public List<Coordinates> findAll() {
        TypedQuery<Coordinates> query = entityManager.createQuery(
                "SELECT c FROM Coordinates c", 
                Coordinates.class);
        return query.getResultList();
    }

    public List<Coordinates> findAll(int page, int size) {
        TypedQuery<Coordinates> query = entityManager.createQuery(
                "SELECT c FROM Coordinates c", 
                Coordinates.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public List<Coordinates> findAll(int page, int size, String sortBy, String sortOrder) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Coordinates> cq = cb.createQuery(Coordinates.class);
        Root<Coordinates> root = cq.from(Coordinates.class);
        
        cq.select(root);
        
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                cq.orderBy(cb.desc(root.get(sortBy)));
            } else {
                cq.orderBy(cb.asc(root.get(sortBy)));
            }
        }
        
        TypedQuery<Coordinates> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    public long count() {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM Coordinates c", 
                Long.class);
        return query.getSingleResult();
    }

    public Coordinates update(Coordinates coordinates) {
        return entityManager.merge(coordinates);
    }

    public void delete(Long id) {
        Coordinates coordinates = entityManager.find(Coordinates.class, id);
        if (coordinates != null) {
            entityManager.remove(coordinates);
        }
    }
}
