package ru.haritonenko.telegrambotminicrm.consumer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.haritonenko.telegrambotminicrm.model.Lead;
import ru.haritonenko.telegrambotminicrm.model.User;
import ru.haritonenko.telegrambotminicrm.repository.LeadRepository;
import ru.haritonenko.telegrambotminicrm.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    @Value("${admin.ids:}")
    private String adminIdsRaw;
    @Value("${app.pagination.default-page:0}")
    private int defaultPage;
    @Value("${app.pagination.default-size:20}")
    private int defaultSize;
    @Value("${app.pagination.max-size:100}")
    private int maxSize;

    private Set<Long> adminIds;
    private final TelegramClient telegramClient;
    private final UserService userService;
    private final LeadRepository leadRepository;
    private static final DateTimeFormatter LEAD_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @PostConstruct
    void initAdmins() {
        if (adminIdsRaw == null || adminIdsRaw.isBlank()) {
            adminIds = Collections.emptySet();
            return;
        }
        adminIds = Arrays.stream(adminIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        log.info("Admin ids initialized: {}", adminIds);
        if (defaultPage < 0) defaultPage = 0;
        if (defaultSize <= 0) defaultSize = 20;
        if (maxSize <= 0) maxSize = 100;
    }

    private int pageSize() {
        return Math.min(defaultSize, maxSize);
    }

    private boolean isAdmin(Long chatId) {
        return adminIds.contains(chatId);
    }

    private void ensureUserExists(Message message) {
        Long chatId = message.getChatId();
        if (userService.getByChatId(chatId).isPresent()) {
            return;
        }
        var from = message.getFrom();
        var user = User.builder()
                .chatId(chatId)
                .username(from != null ? from.getUserName() : null)
                .firstName(from != null ? from.getFirstName() : null)
                .lastName(from != null ? from.getLastName() : null)
                .notify(false)
                .build();
        userService.save(user);
        log.info("New user created: chatId={}", chatId);
    }

    @Override
    public void consume(Update update) {
        if (update == null) return;

        try {
            if (update.hasCallbackQuery()) {
                log.debug("Received callback query");
                handleCallbackQuery(update.getCallbackQuery());
                return;
            }

            if (!update.hasMessage()) return;
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            if (!message.hasText()) return;

            ensureUserExists(message);

            String text = message.getText().trim();
            log.info("Received message from chatId={}: {}", chatId, text);

            String reply;

            if (text.equals("/start") || text.equals("/help")) {
                hideReplyKeyboard(chatId);
                sendMainMenu(chatId);
                return;
            } else if (text.startsWith("/user_id")) {
                reply = handleGetById(text);
            } else if (text.startsWith("/by_phone")) {
                reply = handleByPhone(text);
            } else if (text.equals("/notify_me")) {
                var existing = userService.getByChatId(chatId);
                if (existing.isPresent()) {
                    var u = existing.get();
                    if (Boolean.TRUE.equals(u.getNotify())) {
                        reply = "Вы уже подписаны ✅";
                    } else {
                        u.setNotify(true);
                        if (message.getFrom() != null) {
                            u.setUsername(message.getFrom().getUserName());
                            u.setFirstName(message.getFrom().getFirstName());
                            u.setLastName(message.getFrom().getLastName());
                        }
                        userService.save(u);
                        reply = "Подписка включена ✅";
                    }
                } else {
                    var u = User.builder()
                            .chatId(chatId)
                            .username(message.getFrom() != null ? message.getFrom().getUserName() : null)
                            .firstName(message.getFrom() != null ? message.getFrom().getFirstName() : null)
                            .lastName(message.getFrom() != null ? message.getFrom().getLastName() : null)
                            .notify(true)
                            .build();
                    userService.save(u);
                    reply = "Вы подписаны на рассылку ✅";
                }
                log.info("Notify command processed for chatId={}", chatId);
            } else if (text.equals("/notify_off")) {
                var opt = userService.getByChatId(chatId);
                if (opt.isEmpty()) {
                    reply = "У вас нет активной подписки.";
                } else {
                    var u = opt.get();
                    if (!Boolean.TRUE.equals(u.getNotify())) {
                        reply = "Подписка уже выключена.";
                    } else {
                        u.setNotify(false);
                        userService.save(u);
                        reply = "Рассылка отключена ❌";
                    }
                }
                log.info("Notify off processed for chatId={}", chatId);
            } else if (text.equals("/notify_list")) {
                if (!isAdmin(chatId)) {
                    reply = "Доступ запрещён.";
                    log.warn("Notify list access denied for chatId={}", chatId);
                } else {
                    sendUsersPage(chatId, defaultPage);
                    return;
                }
            } else if (text.startsWith("/remove")) {
                if (!isAdmin(chatId)) {
                    reply = "Доступ запрещён.";
                    log.warn("Remove access denied for chatId={}", chatId);
                } else {
                    String[] parts = text.split("\\s+");
                    if (parts.length < 2) reply = "Использование: /remove <chatId>";
                    else {
                        try {
                            Long target = Long.parseLong(parts[1]);
                            userService.deleteByChatId(target);
                            reply = "Пользователь удалён.";
                            log.info("User with chatId={} removed by admin chatId={}", target, chatId);
                        } catch (NumberFormatException nfe) {
                            reply = "chatId должен быть числом.";
                            log.warn("Invalid chatId format in /remove from chatId={}", chatId);
                        }
                    }
                }
            } else if (text.equals("/stop")) {
                userService.deleteByChatId(chatId);
                reply = """
                        Мы удалили ваши данные и больше не будем писать ✅

                        Если захотите вернуться — просто отправьте /start.
                        """;
                log.info("User requested stop and was deleted: chatId={}", chatId);
            } else if (text.equals("/leads")) {
                sendLeadsPage(chatId, defaultPage);
                return;
            } else {
                reply = "Не знаю такую команду. Напишите /help";
            }

            sendMessage(chatId, reply);
        } catch (Exception e) {
            log.error("Error while processing update", e);
            if (update != null && update.hasMessage()) {
                Long chatId = update.getMessage().getChatId();
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        }
    }

    private String formatUserShort(User u) {
        String uname = u.getUsername() != null ? ("@" + u.getUsername()) : "-";
        String phone = u.getPhone() != null ? UserService.normalizePhone(u.getPhone()) : "-";
        return "%d | chatId=%d | %s | %s | notify=%s"
                .formatted(u.getId(), u.getChatId(), uname, phone, u.getNotify());
    }

    private String handleGetById(String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) return "Укажите id: /user_id <id>";
        Long id = Long.parseLong(parts[1]);
        return userService.getById(id)
                .map(this::formatUserShort)
                .orElse("Не найден пользователь с id=" + id);
    }

    private String handleByPhone(String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) return "Укажите телефон: /by_phone <phone>";
        return userService.getByPhone(parts[1].trim())
                .map(this::formatUserShort)
                .orElse("Не найдено");
    }

    private void sendLeadsPage(Long chatId, int pageNumber) {
        if (pageNumber < 0) {
            sendMessage(chatId, "Заявок нет.");
            return;
        }

        var pageable = PageRequest.of(pageNumber, pageSize(), Sort.by(Sort.Direction.ASC, "id"));
        var page = leadRepository.findAll(pageable);

        if (page.isEmpty()) {
            sendMessage(chatId, "Заявок нет.");
            return;
        }

        List<Lead> leads = page.getContent();

        StringBuilder sb = new StringBuilder("Последние заявки (страница ")
                .append(pageNumber + 1)
                .append("):\n\n");

        for (Lead l : leads) {
            String created = l.getCreatedAt() != null ? l.getCreatedAt().format(LEAD_DT_FMT) : "-";
            sb.append("#").append(l.getId())
                    .append(" | ").append(l.getFio())
                    .append(" | ").append(l.getPhone())
                    .append(" | ").append(l.getDistrict())
                    .append(" | ").append(l.getSource())
                    .append(" | кол-во=").append(l.getQuantity())
                    .append(" | сумма=").append(l.getAmount())
                    .append(" | ").append(created)
                    .append("\n\n");
        }

        InlineKeyboardMarkup keyboard = buildPaginationKeyboard("leads_page", pageNumber, page.hasPrevious(), page.hasNext());
        sendMessage(chatId, sb.toString(), keyboard);
        log.info("Leads page {} sent to chatId={}", pageNumber, chatId);
    }

    private void sendUsersPage(Long chatId, int pageNumber) {
        if (pageNumber < 0) {
            sendMessage(chatId, "Пользователей нет.");
            return;
        }

        var pageable = PageRequest.of(pageNumber, pageSize(), Sort.by(Sort.Direction.ASC, "id"));
        var page = userService.findNotifyOn(pageable);

        if (page.isEmpty()) {
            sendMessage(chatId, "Пользователей нет.");
            return;
        }

        List<User> users = page.getContent();

        StringBuilder sb = new StringBuilder("Подписчики (страница ")
                .append(pageNumber + 1)
                .append("):\n\n");

        for (User u : users) {
            sb.append(formatUserShort(u)).append("\n\n");
        }

        InlineKeyboardMarkup keyboard = buildPaginationKeyboard("users_page", pageNumber, page.hasPrevious(), page.hasNext());
        sendMessage(chatId, sb.toString(), keyboard);
        log.info("Users page {} sent to chatId={}", pageNumber, chatId);
    }

    private InlineKeyboardMarkup buildPaginationKeyboard(String prefix, int pageNumber, boolean hasPrev, boolean hasNext) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> navButtons = new ArrayList<>();

        if (hasPrev) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("⬅️ Previous")
                    .callbackData(prefix + ":" + (pageNumber - 1))
                    .build());
        }
        if (hasNext) {
            navButtons.add(InlineKeyboardButton.builder()
                    .text("Next ➡️")
                    .callbackData(prefix + ":" + (pageNumber + 1))
                    .build());
        }

        if (!navButtons.isEmpty()) {
            rows.add(new InlineKeyboardRow(navButtons));
        }

        return rows.isEmpty() ? null : new InlineKeyboardMarkup(rows);
    }

    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        var message = SendMessage.builder()
                .text("""
                        Доступные команды:
                        
                        /user_id <id>
                        /by_phone <телефон>
                        /leads — последние заявки
                        
                        Подписка на рассылку:
                        /notify_me — подписать текущий чат на рассылку уведомлений
                        /notify_off — отписаться от рассылки
                        /notify_list — список подписанных пользователей (для админов)
                        /remove <chatId> — удалить подписчика (для админов)
                        /stop — прекратить общение и удалить ваши данные
                        
                        """)
                .chatId(chatId)
                .build();

        var keyBoard = new InlineKeyboardMarkup(List.of(
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Поиск по id").callbackData("search_by_id").build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Поиск по телефону").callbackData("search_by_phone").build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Список заявок").callbackData("list_leads").build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Подписчики").callbackData("list_users").build()
                ),
                new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Прекратить общение").callbackData("stop_chat").build()
                )
        ));
        message.setReplyMarkup(keyBoard);
        telegramClient.execute(message);
        log.info("Main menu sent to chatId={}", chatId);
    }

    private void handleCallbackQuery(CallbackQuery cb) {
        Long chatId = cb.getMessage().getChatId();
        String data = cb.getData();
        log.info("Callback query from chatId={}, data={}", chatId, data);

        if (data.startsWith("leads_page:")) {
            int page = Integer.parseInt(data.substring("leads_page:".length()));
            sendLeadsPage(chatId, page);
            return;
        }
        if (data.startsWith("users_page:")) {
            int page = Integer.parseInt(data.substring("users_page:".length()));
            sendUsersPage(chatId, page);
            return;
        }

        switch (data) {
            case "search_by_id" ->
                    sendMessage(chatId, "Введите: /user_id <id>\nНапример: /user_id 1");
            case "search_by_phone" ->
                    sendMessage(chatId, "Введите: /by_phone <телефон>\nНапример: /by_phone +7(999)123-45-67");
            case "list_leads" -> {
                sendLeadsPage(chatId, defaultPage);
                log.info("Leads list sent via callback to chatId={}", chatId);
            }
            case "list_users" -> {
                if (!isAdmin(chatId)) {
                    sendMessage(chatId, "Доступ запрещён.");
                    log.warn("list_users access denied for chatId={}", chatId);
                } else {
                    sendUsersPage(chatId, defaultPage);
                }
            }
            case "stop_chat" -> {
                userService.deleteByChatId(chatId);
                sendMessage(chatId, """
                        Мы удалили ваши данные и больше не будем писать ✅

                        Если захотите вернуться — просто отправьте /start.
                        """);
                log.info("User deleted via stop_chat callback: chatId={}", chatId);
            }
            default ->
                    sendMessage(chatId, "Неизвестная команда");
        }
    }

    @SneakyThrows
    private void sendMessage(Long chatId, String messageText) {
        sendMessage(chatId, messageText, null);
    }

    @SneakyThrows
    private void sendMessage(Long chatId, String messageText, InlineKeyboardMarkup keyboard) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .text(messageText)
                .chatId(chatId.toString());
        if (keyboard != null) {
            builder.replyMarkup(keyboard);
        }
        telegramClient.execute(builder.build());
        log.debug("Sent message to chatId={}", chatId);
    }

    @SneakyThrows
    private void hideReplyKeyboard(Long chatId) {
        var message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Добро пожаловать!")
                .replyMarkup(new ReplyKeyboardRemove(true))
                .build();
        telegramClient.execute(message);
        log.debug("Reply keyboard removed for chatId={}", chatId);
    }
}
