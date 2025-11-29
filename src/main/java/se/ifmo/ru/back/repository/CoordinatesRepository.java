package se.ifmo.ru.back.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.ifmo.ru.back.entity.Coordinates;

import java.util.List;

@Repository
public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
    
    // Автоматически генерируемые методы Spring Data JPA
    // findById, save, delete, findAll, count - уже есть в JpaRepository
    
    // Пагинация
    Page<Coordinates> findAll(Pageable pageable);
    
    // Сортировка и пагинация
    List<Coordinates> findAll(Sort sort);
}

