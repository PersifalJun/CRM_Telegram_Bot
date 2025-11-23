package ru.haritonenko.telegrambotminicrm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.haritonenko.telegrambotminicrm.dto.LeadRequest;
import ru.haritonenko.telegrambotminicrm.model.Lead;
import ru.haritonenko.telegrambotminicrm.model.User;
import ru.haritonenko.telegrambotminicrm.repository.LeadRepository;
import ru.haritonenko.telegrambotminicrm.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TelegramClient telegramClient;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;

    public boolean broadcastLead(LeadRequest request) {
        log.info("Received lead request: phone={}, fio={}, amount={}", request.phone(), request.fio(), request.amount());

        boolean exists = leadRepository.existsByPhoneEquivalent(request.phone());
        if (exists) {
            log.info("Lead with equivalent phone already exists, skipping: phone={}", request.phone());
            return false;
        }

        Lead lead = Lead.builder()
                .fio(request.fio())
                .phone(request.phone())
                .district(request.district())
                .source(request.source())
                .quantity(request.quantity())
                .amount(request.amount())
                .build();
        lead = leadRepository.save(lead);

        log.info("Lead saved: id={}, phone={}", lead.getId(), lead.getPhone());

        List<User> targets = userRepository.findByNotifyTrue();
        log.info("Found {} users with notify=true", targets.size());

        String text = """
                🔔 Новая заявка #%d
                ФИО: %s
                Телефон: %s
                Район: %s
                Source: %s
                Кол-во: %d
                Сумма: %s
                """.formatted(
                lead.getId(), lead.getFio(), lead.getPhone(),
                lead.getDistrict(), lead.getSource(), lead.getQuantity(), lead.getAmount()
        );

        for (User u : targets) {
            if (u.getChatId() == null) continue;
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(u.getChatId().toString())
                        .text(text)
                        .build());
                log.debug("Lead notification sent to chatId={}", u.getChatId());
            } catch (Exception e) {
                log.error("Error sending lead notification to chatId={}", u.getChatId(), e);
            }
        }

        return true;
    }
}
