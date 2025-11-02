package ru.haritonenko.telegrambotminicrm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table( schema = "minicrm",
        name = "crm_user",
        indexes = {
                @Index(name = "ix_crm_user_district", columnList = "district"),
                @Index(name = "ix_crm_user_source",   columnList = "source")
        }
)
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 2, max = 150)
    private String fio;

    @NotBlank(message = "Необходимо указать телефон")
    @Pattern(regexp = "^[+\\d][\\d\\s()\\-]{8,}$")
    @Column(nullable = false)
    private String phone;

    @NotBlank(message = "Необходимо указать место проживания")
    private String district;

    @NotBlank(message = "Необходимо указать источник")
    private String source;

    @NotNull @Min(0)
    private Integer quantity = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
