package dot.ray.table;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "guild_channels")
public class ChannelMeta {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "timer_sec")
    private Long publicationTimer;

    @Column(name = "immune_posters")
    @ElementCollection
    private List<String> immune;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long guildId) {
        this.guildId = guildId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getPublicationTimer() {
        return publicationTimer;
    }

    public void setPublicationTimer(Long timerSecond) {
        this.publicationTimer = timerSecond;
    }

    public List<String> getImmune() {
        return immune;
    }

    public void setImmune(List<String> immune) {
        this.immune = immune;
    }
}
