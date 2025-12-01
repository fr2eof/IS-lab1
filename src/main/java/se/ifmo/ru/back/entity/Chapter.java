package se.ifmo.ru.back.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import se.ifmo.ru.back.validation.UniqueFields;

@Entity
@Table(name = "chapters")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@UniqueFields(fields = {"name", "marinesCount"}, message = "Глава с таким именем и количеством маринов уже существует")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_seq")
    @SequenceGenerator(name = "chapter_seq", sequenceName = "chapter_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Chapter name cannot be null or empty")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Marines count cannot be null")
    @Min(value = 1, message = "Marines count must be greater than 0")
    @Max(value = 1000, message = "Marines count cannot exceed 1000")
    @Column(name = "marines_count", nullable = false)
    private int marinesCount;

    @Version
    @Column(name = "version")
    private Long version;
}
