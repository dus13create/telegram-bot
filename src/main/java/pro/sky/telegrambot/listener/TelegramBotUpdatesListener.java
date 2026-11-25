package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pro.sky.telegrambot.model.Reminder;
import pro.sky.telegrambot.repository.ReminderRepository;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private ReminderRepository reminderRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    private static final Pattern REMINDER_PATTERN = Pattern.compile(
            "(\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2})\\s+(.*)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private ReplyKeyboardMarkup createMainKeyboard() {
        KeyboardButton activeRemindersButton = new KeyboardButton("активные напоминания");
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(
                new KeyboardButton[]{activeRemindersButton}
        );
        keyboardMarkup.resizeKeyboard(true);
        return keyboardMarkup;
    }

    private void sendActiveReminders(Long chatId) {
        List<Reminder> activeReminders = reminderRepository.findByChatIdAndIsSentFalseOrderByRemindAtAsc(chatId);
        String responseText;
        if (activeReminders.isEmpty()) {
            responseText = "Активных напоминаний пока нет.";
        } else {
            StringBuilder sb = new StringBuilder("Ваши активные напоминания:\n\n");
            for (int i = 0; i < activeReminders.size(); i++) {
                Reminder r = activeReminders.get(i);
                String formattedDate = r.getRemindAt().format(DATE_TIME_FORMATTER);
                sb.append(i + 1)
                  .append(". ")
                  .append(formattedDate)
                  .append(" — ")
                  .append(r.getText())
                  .append("\n");
            }
            responseText = sb.toString();
        }
        SendMessage message = new SendMessage(chatId, responseText).replyMarkup(createMainKeyboard());
        telegramBot.execute(message);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() == null) {
                return;
            }
            String text = update.message().text();
            Long chatId = update.message().chat().id();

            if ("/start".equals(text)) {
                SendMessage message = new SendMessage(
                        chatId,
                        "Добро пожаловать! Я бот для напоминаний.\n" +
                        "Чтобы создать напоминание, отправьте сообщение в формате:\n" +
                        "дд.ММ.гггг ЧЧ:мм Текст напоминания\n\n" +
                        "Нажмите кнопку «активные напоминания» или введите /list, чтобы посмотреть список."
                ).replyMarkup(createMainKeyboard());
                telegramBot.execute(message);
            } else if ("/list".equals(text) || "/reminders".equals(text) || "активные напоминания".equalsIgnoreCase(text.trim())) {
                sendActiveReminders(chatId);
            } else {
                Matcher matcher = REMINDER_PATTERN.matcher(text);
                if (matcher.matches()) {
                    LocalDateTime remindAt = LocalDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);
                    String reminderText = matcher.group(2);
                    reminderRepository.save(new Reminder(chatId, reminderText, remindAt));
                    telegramBot.execute(new SendMessage(chatId,
                            "Напоминание сохранено: " + reminderText + " на " + matcher.group(1)));
                } else {
                    telegramBot.execute(new SendMessage(chatId,
                            "Формат: ДД.ММ.ГГГГ ЧЧ:ММ текст напоминания\nПример: 26.11.2026 10:00 Проверить напоминание"));
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
