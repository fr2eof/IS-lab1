package se.ifmo.ru.back.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.ifmo.ru.back.entity.SpaceMarine;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceMarineRepository extends JpaRepository<SpaceMarine, Integer> {

    // Кастомные запросы с JOIN FETCH для загрузки связанных сущностей
    @Query("SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter")
    List<SpaceMarine> findAllWithRelations();
    
    @Query("SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter")
    Page<SpaceMarine> findAllWithRelations(Pageable pageable);
    
    // Поиск по имени (содержит подстроку)
    @Query("SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter " +
           "WHERE sm.name LIKE CONCAT('%', :name, '%')")
    List<SpaceMarine> findByNameContaining(@Param("name") String name);
    
    // Поиск по здоровью (меньше чем)
    @Query("SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter " +
           "WHERE sm.health < :health")
    List<SpaceMarine> findByHealthLessThan(@Param("health") Integer health);
    
    // Подсчет по здоровью
    long countByHealthLessThan(Integer health);
    
    // Среднее значение heartCount
    @Query("SELECT AVG(sm.heartCount) FROM SpaceMarine sm")
    Double getAverageHeartCount();
    
    // Поиск по координатам
    @Query("SELECT sm FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId")
    List<SpaceMarine> findByCoordinatesId(@Param("coordinatesId") Long coordinatesId);
    
    // Поиск по главе
    @Query("SELECT sm FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId")
    List<SpaceMarine> findByChapterId(@Param("chapterId") Long chapterId);
    
    // Подсчет по координатам
    @Query("SELECT COUNT(sm) FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId")
    long countByCoordinatesId(@Param("coordinatesId") Long coordinatesId);
    
    // Подсчет по главе
    @Query("SELECT COUNT(sm) FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId")
    long countByChapterId(@Param("chapterId") Long chapterId);
    
    // Удаление по главе
    @Modifying
    @Transactional
    @Query("DELETE FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId")
    void deleteByChapterId(@Param("chapterId") Long chapterId);
    
    // Удаление по координатам
    @Modifying
    @Transactional
    @Query("DELETE FROM SpaceMarine sm WHERE sm.coordinates.id = :coordinatesId")
    void deleteByCoordinatesId(@Param("coordinatesId") Long coordinatesId);
    
    // Поиск с фильтрами по имени (точное совпадение, case-insensitive)
    @Query("SELECT sm FROM SpaceMarine sm LEFT JOIN FETCH sm.coordinates LEFT JOIN FETCH sm.chapter " +
           "WHERE LOWER(sm.name) = LOWER(:nameFilter)")
    Page<SpaceMarine> findWithNameFilter(@Param("nameFilter") String nameFilter, Pageable pageable);
    
    // Подсчет с фильтром по имени
    @Query("SELECT COUNT(sm) FROM SpaceMarine sm WHERE LOWER(sm.name) = LOWER(:nameFilter)")
    long countWithNameFilter(@Param("nameFilter") String nameFilter);
    
    // Проверка уникальности с блокировкой для предотвращения race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sm FROM SpaceMarine sm WHERE sm.chapter.id = :chapterId AND sm.health = :health " +
           "AND sm.weaponType = :weaponType AND sm.coordinates.id = :coordinatesId")
    List<SpaceMarine> findByChapterAndHealthAndWeaponAndCoordinatesWithLock(
            @Param("chapterId") Long chapterId,
            @Param("health") Integer health,
            @Param("weaponType") String weaponType,
            @Param("coordinatesId") Long coordinatesId);
    
    // Поиск с блокировкой для обновления
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sm FROM SpaceMarine sm WHERE sm.id = :id")
    Optional<SpaceMarine> findByIdForUpdate(@Param("id") Integer id);
    
    // Метод для обновления (используется через save из JpaRepository)
    // update не нужен, так как save() автоматически делает merge для существующих сущностей
}

