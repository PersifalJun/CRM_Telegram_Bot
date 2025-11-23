package ru.haritonenko.telegrambotminicrm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.haritonenko.telegrambotminicrm.exceptions.UserDeleteException;
import ru.haritonenko.telegrambotminicrm.exceptions.UserNotFoundException;
import ru.haritonenko.telegrambotminicrm.model.User;
import ru.haritonenko.telegrambotminicrm.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Validated
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return phone;
        if (digits.length() == 11 && digits.startsWith("8")) digits = "7" + digits.substring(1);
        if (digits.length() == 10) digits = "7" + digits;
        return "+" + digits;
    }

    @Transactional(readOnly = true)
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    @Transactional(readOnly = true)
    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> getByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional(readOnly = true)
    public List<User> findNotifyOn() {
        return userRepository.findByNotifyTrue();
    }

    @Transactional(readOnly = true)
    public Page<User> findNotifyOn(Pageable pageable) {
        return userRepository.findByNotifyTrue(pageable);
    }

    @Transactional
    public User save(User u) {
        User saved = userRepository.save(u);
        log.info("User saved: id={}, chatId={}", saved.getId(), saved.getChatId());
        return saved;
    }

    @Transactional(readOnly = true)
    public User getRequiredByChatId(Long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    log.warn("User with chatId={} not found", chatId);
                    return new UserNotFoundException("User with chatId=" + chatId + " not found");
                });
    }

    @Transactional
    public void deleteByChatId(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    log.warn("Attempt to delete non-existing user with chatId={}", chatId);
                    return new UserNotFoundException("User with chatId=" + chatId + " not found");
                });
        try {
            userRepository.delete(user);
            log.info("User deleted: id={}, chatId={}", user.getId(), chatId);
        } catch (Exception e) {
            log.error("Error deleting user with chatId={}", chatId, e);
            throw new UserDeleteException("Error deleting user with chatId=" + chatId, e);
        }
    }
}
