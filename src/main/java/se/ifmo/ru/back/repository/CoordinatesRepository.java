package se.ifmo.ru.back.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.ifmo.ru.back.entity.Coordinates;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
    
    // Автоматически генерируемые методы Spring Data JPA
    // findById, save, delete, findAll, count - уже есть в JpaRepository
    
    // Пагинация
    @QueryHints(@jakarta.persistence.QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    Page<Coordinates> findAll(Pageable pageable);
    
    // Сортировка и пагинация
    @QueryHints(@jakarta.persistence.QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"))
    List<Coordinates> findAll(Sort sort);
    
    // Проверка уникальности с блокировкой для предотвращения race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coordinates c WHERE c.x = :x AND (c.y = :y OR (c.y IS NULL AND :y IS NULL))")
    Optional<Coordinates> findByXAndYWithLock(@Param("x") Float x, @Param("y") Double y);
    
    // Поиск с блокировкой для обновления
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coordinates c WHERE c.id = :id")
    Optional<Coordinates> findByIdForUpdate(@Param("id") Long id);
}

