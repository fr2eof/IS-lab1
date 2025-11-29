package se.ifmo.ru.back.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.ifmo.ru.back.entity.ImportHistory;

import java.util.List;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {
    
    // Автоматически генерируемые методы Spring Data JPA
    // findById, save, delete, findAll, count - уже есть в JpaRepository
    
    // Пагинация
    Page<ImportHistory> findAll(Pageable pageable);
    
    // Поиск всех с сортировкой
    List<ImportHistory> findAll(Sort sort);
    
    // Поиск по имени пользователя
    List<ImportHistory> findByUsernameOrderByCreatedAtDesc(String username);
    
    // Поиск по имени пользователя с пагинацией
    Page<ImportHistory> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    
    // Подсчет по имени пользователя
    long countByUsername(String username);
}

