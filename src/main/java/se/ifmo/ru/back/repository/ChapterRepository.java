package se.ifmo.ru.back.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.entity.Chapter;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    
    // Автоматически генерируемые методы Spring Data JPA
    // findById, save, delete, findAll, count - уже есть в JpaRepository
    
    // Поиск по имени
    Optional<Chapter> findByName(String name);
    
    // Пагинация
    Page<Chapter> findAll(Pageable pageable);
    
    // Сортировка и пагинация
    List<Chapter> findAll(Sort sort);
    
    // Проверка уникальности с блокировкой для предотвращения race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Chapter c WHERE c.name = :name AND c.marinesCount = :marinesCount")
    Optional<Chapter> findByNameAndMarinesCountWithLock(@Param("name") String name, @Param("marinesCount") int marinesCount);
    
    // Поиск с блокировкой для обновления
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Chapter c WHERE c.id = :id")
    Optional<Chapter> findByIdForUpdate(@Param("id") Long id);
    
    // Методы для работы со счетчиком маринов
    @Modifying
    @Transactional
    @Query("UPDATE Chapter c SET c.marinesCount = c.marinesCount + 1 WHERE c.id = :chapterId AND c.marinesCount < 1000")
    int addMarineToChapter(@Param("chapterId") Long chapterId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Chapter c SET c.marinesCount = c.marinesCount - 1 WHERE c.id = :chapterId AND c.marinesCount > 0")
    int removeMarineFromChapter(@Param("chapterId") Long chapterId);
}

