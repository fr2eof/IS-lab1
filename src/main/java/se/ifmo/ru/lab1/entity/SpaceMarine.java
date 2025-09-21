package se.ifmo.ru.lab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "space_marines")
public class SpaceMarine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotBlank(message = "Name cannot be null or empty")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Coordinates cannot be null")
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull(message = "Creation date cannot be null")
    @Column(name = "creation_date", nullable = false)
    private ZonedDateTime creationDate;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @NotNull(message = "Health cannot be null")
    @Min(value = 1, message = "Health must be greater than 0")
    @Column(name = "health", nullable = false)
    private Integer health;

    @NotNull(message = "Heart count cannot be null")
    @Min(value = 1, message = "Heart count must be greater than 0")
    @Max(value = 3, message = "Heart count cannot exceed 3")
    @Column(name = "heart_count", nullable = false)
    private Integer heartCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private AstartesCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "weapon_type")
    private Weapon weaponType;

    public SpaceMarine() {
        this.creationDate = ZonedDateTime.now();
    }

    public SpaceMarine(String name, Coordinates coordinates, Chapter chapter, 
                      Integer health, Integer heartCount, AstartesCategory category, Weapon weaponType) {
        this();
        this.name = name;
        this.coordinates = coordinates;
        this.chapter = chapter;
        this.health = health;
        this.heartCount = heartCount;
        this.category = category;
        this.weaponType = weaponType;
    }

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = ZonedDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getHeartCount() {
        return heartCount;
    }

    public void setHeartCount(Integer heartCount) {
        this.heartCount = heartCount;
    }

    public AstartesCategory getCategory() {
        return category;
    }

    public void setCategory(AstartesCategory category) {
        this.category = category;
    }

    public Weapon getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(Weapon weaponType) {
        this.weaponType = weaponType;
    }

    @Override
    public String toString() {
        return "SpaceMarine{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", chapter=" + chapter +
                ", health=" + health +
                ", heartCount=" + heartCount +
                ", category=" + category +
                ", weaponType=" + weaponType +
                '}';
    }
}
