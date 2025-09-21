# Space Marine Management System

Информационная система для управления объектами класса SpaceMarine, разработанная с использованием Jakarta EE, CDI и Hibernate.

## Описание системы

Система позволяет выполнять следующие операции с объектами SpaceMarine:
- Создание новых объектов
- Получение информации об объекте по ID
- Обновление объектов (модификация атрибутов)
- Удаление объектов
- Фильтрация и сортировка объектов
- Специальные операции (статистика, поиск)

## Технологический стек

- **Backend**: Jakarta EE 11, CDI, JPA/Hibernate
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Application Server**: Jakarta EE compatible server (WildFly)


## Настройка базы данных

1. Убедитесь, что PostgreSQL запущен
2. Создайте базу данных `studs` (если не существует)
3. Выполните SQL скрипт с функциями:
   ```sql
   -- Выполните содержимое файла src/main/resources/db/functions.sql
   ```

## Конфигурация подключения к БД

Настройки подключения находятся в `src/main/resources/META-INF/persistence.xml`:

```xml
<property name="jakarta.persistence.jdbc.url" value="jdbc:postgresql://pg:5432/studs"/>
<property name="jakarta.persistence.jdbc.user" value="studs"/>
<property name="jakarta.persistence.jdbc.password" value="studs"/>
```

## Сборка и развертывание

1. **Сборка проекта**:
   ```bash
   mvn clean package
   ```

2. **Развертывание**:
   - Скопируйте `target/lab1.war` в директорию развертывания вашего сервера приложений
   - Или используйте Maven плагины для автоматического развертывания

3. **Доступ к приложению**:
   - Откройте браузер и перейдите по адресу: `http://localhost:8080/lab1/`

## Функциональность

### Основные операции
- **CRUD операции** для SpaceMarine объектов
- **Управление Chapter** (орденами космодесанта)
- **Валидация данных** на уровне JPA и REST API
- **Пагинация** и фильтрация в таблице

### Специальные операции
1. **Расчет среднего значения поля heartCount** для всех объектов
2. **Подсчет объектов** с health меньше заданного значения
3. **Поиск объектов** по подстроке в поле name
4. **Создание нового ордена** космодесанта
5. **Отчисление десантника** из указанного ордена

## API Endpoints

### SpaceMarine API
- `GET /api/spacemarines` - Получить список с пагинацией и фильтрацией
- `GET /api/spacemarines/{id}` - Получить по ID
- `POST /api/spacemarines` - Создать новый
- `PUT /api/spacemarines/{id}` - Обновить
- `DELETE /api/spacemarines/{id}` - Удалить

### Chapter API
- `GET /api/chapters` - Получить все ордена
- `GET /api/chapters/{id}` - Получить по ID
- `POST /api/chapters` - Создать новый орден
- `PUT /api/chapters/{id}` - Обновить орден
- `DELETE /api/chapters/{id}` - Удалить орден

### Special Operations API
- `GET /api/special-operations/average-heart-count` - Среднее значение heartCount
- `GET /api/special-operations/count-by-health?health={value}` - Подсчет по health
- `GET /api/special-operations/search-by-name?name={value}` - Поиск по имени
- `POST /api/special-operations/create-chapter` - Создать орден через функцию БД
- `POST /api/special-operations/remove-marine-from-chapter` - Отчислить десантника

## Требования к системе

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Jakarta EE 11 compatible application server
