package dot.rey.table;

import javax.persistence.*;

@Entity
@Table(name = "user_bans", uniqueConstraints = {@UniqueConstraint(columnNames = {"channel_id", "user_id"})})
public class BanTable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private GuildMetaTable guildMetaTable;

    public BanTable() {
    }

    public BanTable(Long channelId, Long userId, GuildMetaTable guildMetaTable) {
        this.channelId = channelId;
        this.userId = userId;
        this.guildMetaTable = guildMetaTable;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
