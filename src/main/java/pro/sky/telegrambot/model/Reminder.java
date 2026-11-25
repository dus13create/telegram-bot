package pro.sky.telegrambot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String text;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Reminder(Long chatId, String text, LocalDateTime remindAt) {
        this.chatId = chatId;
        this.text = text;
        this.remindAt = remindAt;
        this.createdAt = LocalDateTime.now();
        this.isSent = false;
    }
}
