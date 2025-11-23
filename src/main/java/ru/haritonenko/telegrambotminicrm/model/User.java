package ru.haritonenko.telegrambotminicrm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        schema = "minicrm",
        name = "tg_users",
        indexes = {
                @Index(name = "ix_tg_user_notify", columnList = "notify")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_tg_user_chat_id", columnNames = {"chat_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Size(max = 50, message = "Максимальная длина никнейма 60 знаков")
    @Column(name = "username")
    private String username;

    @Size(max = 15, message = "Максимальная длина символов телефона 15 знаков")
    @Column(name = "phone")
    private String phone;

    @Size(max = 50, message = "Максимальная длина символов имени 50 знаков")
    @Column(name = "first_name")
    private String firstName;
    @Size(max = 50, message = "Максимальная длина фамилии 50 знаков")
    @Column(name = "last_name")
    private String lastName;

    @Column(name = "notify", nullable = false)
    private Boolean notify = false;

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
