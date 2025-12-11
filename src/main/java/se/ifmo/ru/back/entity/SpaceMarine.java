package se.ifmo.ru.back.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import se.ifmo.ru.back.validation.UniqueSpaceMarineInChapter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "space_marines")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "se.ifmo.ru.back.entity.SpaceMarine")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"coordinates", "chapter"})
@UniqueSpaceMarineInChapter
public class SpaceMarine {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "space_marine_seq")
    @SequenceGenerator(name = "space_marine_seq", sequenceName = "space_marine_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotBlank(message = "Name cannot be null or empty")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Coordinates cannot be null")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    // NOTE: Multiple SpaceMarines can share the same coordinates according to requirements
    // If constraint violation occurs, unique constraint may need to be removed from DB
    private Coordinates coordinates;

    @NotNull(message = "Creation date cannot be null")
    @Column(name = "creation_date", nullable = false)
    private ZonedDateTime creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chapter_id")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = ZonedDateTime.now();
        }
    }
}
