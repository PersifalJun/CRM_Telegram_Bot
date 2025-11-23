package ru.haritonenko.telegrambotminicrm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.telegrambotminicrm.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
    List<User> findByNotifyTrue();
    Page<User> findByNotifyTrue(Pageable pageable);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByPhone(String phone);
}
