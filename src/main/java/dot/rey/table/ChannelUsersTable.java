package dot.rey.table;

import javax.persistence.*;

@Entity
@Table(name = "channel_users", uniqueConstraints = {@UniqueConstraint(columnNames = {"channel_id", "user_id"})})
public class ChannelUsersTable {

    //will be much better to save user-channel-permitsInt
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "is_moderator")
    private boolean isModerator = false;

    @Column(name = "is_banned")
    private boolean isBanned = false;

    @Column(name = "is_Owner")
    private boolean isOwner = false;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private GuildMetaTable guildMetaTable;

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

    public boolean isModerator() {
        return isModerator;
    }

    public void setModerator(boolean moderator) {
        isModerator = moderator;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public GuildMetaTable getGuildMetaTable() {
        return guildMetaTable;
    }

    public void setGuildMetaTable(GuildMetaTable guildMetaTable) {
        this.guildMetaTable = guildMetaTable;
    }
}
