package ru.haritonenko.telegrambotminicrm.consumer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.haritonenko.telegrambotminicrm.model.User;
import ru.haritonenko.telegrambotminicrm.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    @Value("${app.pagination.default-page:0}")
    private int defaultPage;

    @Value("${app.pagination.default-size:20}")
    private int defaultSize;

    @Value("${app.pagination.max-size:100}")
    private int maxSize;

    private final TelegramClient telegramClient;
    private final UserService userService;

    @Override
    public void consume(Update update) {
        if (update == null) return;

        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage()) {
            Message msg = update.getMessage();
            if (!msg.hasText()) return;

            String text = msg.getText().trim();
            Long chatId = msg.getChatId();

            try {
                String reply;
                if (text.equals("/start") || text.equals("/help")) {
                    sendMainMenu(chatId);
                    return;
                } else if (text.startsWith("/add")) {
                    reply = handleAdd(text);
                } else if (text.startsWith("/user_id")) {
                    reply = handleGetById(text);
                } else if (text.startsWith("/by_phone")) {
                    reply = handleByPhone(text);
                } else if (text.startsWith("/by_district")) {
                    reply = handleByDistrict(text);
                } else if (text.startsWith("/by_source")) {
                    reply = handleBySource(text); // <-- было handleByDistrict
                } else {
                    reply = "Не знаю такую команду. Напишите /help";
                }
                sendMessage(chatId, reply);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        }
    }

    private String handleAdd(String text) {
        String payload = text.length() > 4 ? text.substring(4).trim() : "";
        String[] parts = payload.split(";");
        if (parts.length < 5) {
            return """
                    Неверный формат. Пример:
                    /add Иванов Иван; +7(999)123-45-67; ЦАО; Telegram; 2
                    """;
        }
        String fio = parts[0].trim();
        String phone = parts[1].trim();
        String district = parts[2].trim();
        String source = parts[3].trim();
        int quantity;
        try {
            quantity = Integer.parseInt(parts[4].trim());
        } catch (NumberFormatException e) {
            return "quantity должно быть целым числом.";
        }

        var u = userService.addUser(fio, phone, district, source, quantity);

        return """
                Пользователь добавлен ✅
                id=%d
                ФИО=%s
                Телефон=%s
                Район=%s
                Source=%s
                Кол-во=%d
                """.formatted(
                u.getId(), u.getFio(),
                UserService.normalizePhone(u.getPhone()),
                u.getDistrict(), u.getSource(), u.getQuantity()
        );
    }

    private String handleGetById(String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) return "Укажите id: /user_id 42";
        Long id = Long.parseLong(parts[1]);
        return userService.getById(id)
                .map(this::formatUser)
                .orElse("Не найден пользователь с id=" + id);
    }

    private String handleByPhone(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) return "Укажите телефон: /by_phone +79991234567";
        var list = userService.findByPhone(parts[1].trim());
        if (list.isEmpty()) return "Пользователи не найдены";
        return list.stream().map(this::formatUser).collect(Collectors.joining("\n\n"));
    }

    private String handleByDistrict(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) return "Укажите район: /by_district ЦАО";
        int size = Math.min(defaultSize, maxSize);
        var page = userService.findByDistrict(parts[1].trim(), defaultPage, size);
        if (page.isEmpty()) return "Ничего не найдено";
        return page.stream().map(this::formatUser).collect(Collectors.joining("\n\n"));
    }

    private String handleBySource(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) return "Укажите source: /by_source Telegram";
        int size = Math.min(defaultSize, maxSize);
        var page = userService.findBySource(parts[1].trim(), defaultPage, size);
        if (page.isEmpty()) return "Ничего не найдено";
        return page.stream().map(this::formatUser).collect(Collectors.joining("\n\n"));
    }

    private String formatUser(User u) {
        return "id=%d\nФИО=%s\nТелефон=%s\nРайон=%s\nSource=%s\nКол-во=%d"
                .formatted(
                        u.getId(), u.getFio(),
                        UserService.normalizePhone(u.getPhone()),
                        u.getDistrict(), u.getSource(), u.getQuantity()
                );
    }


    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        var message = SendMessage.builder()
                .text("""
                                        Добро пожаловать! Выберите действие:
                        1).Добавить пользователя
                        2).Поиск пользователя по id
                        3).Поиск пользователя по телефону
                        4).Поиск пользователя по району 
                        5).Поиск пользователя по source""")
                .chatId(chatId)
                .build();

        var kb = new InlineKeyboardMarkup(List.of(
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Добавить пользователя").callbackData("add_user").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Поиск пользователя по id").callbackData("search_by_id").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Поиск пользователя по телефону").callbackData("search_by_phone").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Поиск пользователя по району").callbackData("search_by_district").build()),
                new InlineKeyboardRow(InlineKeyboardButton.builder().text("Поиск пользователя по source").callbackData("search_by_source").build())
        ));
        message.setReplyMarkup(kb);
        telegramClient.execute(message);
    }

    private void handleCallbackQuery(CallbackQuery cb) {
        Long chatId = cb.getMessage().getChatId();
        String data = cb.getData();

        switch (data) {
            case "add_user" ->
                    sendMessage(chatId, "Введите команду в чат в одном сообщении:\n/add ФИО;телефон;район;source;quantity");
            case "search_by_id" -> sendMessage(chatId, "Введите: /user_id <id>\nНапример: /user_id 1");
            case "search_by_phone" ->
                    sendMessage(chatId, "Введите: /by_phone <телефон>\nНапример: /by_phone +7(999)123-45-67");
            case "search_by_district" ->
                    sendMessage(chatId, "Введите: /by_district <район>\nНапример: /by_district ЦАО");
            case "search_by_source" ->
                    sendMessage(chatId, "Введите: /by_source <source>\nНапример: /by_source Telegram");
            default -> sendMessage(chatId, "Неизвестная команда");
        }
    }

    @SneakyThrows
    private void sendMessage(Long chatId, String messageText) {
        telegramClient.execute(SendMessage.builder()
                .text(messageText)
                .chatId(chatId.toString())
                .build());
    }
}
