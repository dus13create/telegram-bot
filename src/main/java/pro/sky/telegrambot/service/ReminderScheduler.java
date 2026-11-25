package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.Reminder;
import pro.sky.telegrambot.repository.ReminderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderScheduler {

    private final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminderRepository;
    private final TelegramBot telegramBot;

    public ReminderScheduler(ReminderRepository reminderRepository, TelegramBot telegramBot) {
        this.reminderRepository = reminderRepository;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        List<Reminder> dueReminders = reminderRepository.findByIsSentFalseAndRemindAtBefore(now);

        for (Reminder reminder : dueReminders) {
            try {
                telegramBot.execute(new SendMessage(reminder.getChatId(), reminder.getText()));
                reminder.setIsSent(true);
                reminderRepository.save(reminder);
                logger.info("Reminder sent: chatId={}, text={}", reminder.getChatId(), reminder.getText());
            } catch (Exception e) {
                logger.error("Failed to send reminder: chatId={}, text={}", reminder.getChatId(), reminder.getText(), e);
            }
        }
    }
}
