package ru.haritonenko.telegrambotminicrm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(schema = "minicrm", name = "leads",
        indexes = {
                @Index(name = "ix_lead_created_at", columnList = "created_at"),
                @Index(name = "ix_lead_source", columnList = "source"),
                @Index(name = "ix_lead_district", columnList = "district")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100, message = "Максимальная длина символов ФИО 100 знаков")
    private String fio;

    @NotBlank
    @Size(max = 15, message = "Максимальный длина символов телефона 15 знаков")
    private String phone;

    @NotBlank
    @Size(max = 50, message = "Максимальная длина символов 50 знаков")
    private String district;

    @NotBlank
    @Size(max = 80, message = "Максимальная длина символов источника 80 знаков")
    private String source;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
