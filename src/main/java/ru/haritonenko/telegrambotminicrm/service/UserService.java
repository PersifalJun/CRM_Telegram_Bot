package ru.haritonenko.telegrambotminicrm.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.haritonenko.telegrambotminicrm.model.User;
import ru.haritonenko.telegrambotminicrm.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Validated
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return phone;
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = "7" + digits;
        }
        return "+" + digits;
    }

    @Transactional
    public User addUser(@NotBlank String fio,@NotBlank String phone, @NotBlank String district, @NotBlank String source, @NotNull Integer quantity) {
        User user = User.builder().
                fio(fio).
                phone(phone).
                district(district).
                source(source).
                quantity(quantity).
                build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<User> findByPhone(String phone) {
        return userRepository.findByPhoneEquivalent(phone);
    }

    @Transactional(readOnly = true)
    public Page<User> findByDistrict(String district, int page, int size) {
        return userRepository.findByDistrictIgnoreCase(district, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<User> findBySource(String source, int page, int size) {
        return userRepository.findBySourceIgnoreCase(source, PageRequest.of(page, size));
    }
}
