package se.ifmo.ru.lab1.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "chapters")
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

    public Chapter() {}

    public Chapter(String name, int marinesCount) {
        this.name = name;
        this.marinesCount = marinesCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMarinesCount() {
        return marinesCount;
    }

    public void setMarinesCount(int marinesCount) {
        this.marinesCount = marinesCount;
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", marinesCount=" + marinesCount +
                '}';
    }
}
