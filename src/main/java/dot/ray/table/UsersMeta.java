package dot.ray.table;

import javax.persistence.*;

@Entity
@Table(name = "user_meta")
public class UsersMeta {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "telegram_id")
    private Long telegramId;

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long discordId) {
        this.guildId = discordId;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }
}
