package se.ifmo.ru.back.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import se.ifmo.ru.back.validation.UniqueFields;

@Entity
@Table(name = "coordinates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@UniqueFields(fields = {"x", "y"}, message = "Координаты с такими значениями x и y уже существуют")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coordinates_seq")
    @SequenceGenerator(name = "coordinates_seq", sequenceName = "coordinates_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "X coordinate cannot be null")
    @Column(name = "x", nullable = false)
    private Float x;

    @Column(name = "y")
    private Double y;
}
