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

    @Column(name = "privilege")
    private Integer privilege = 0;

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

    public Integer getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Integer moderator) {
        privilege = moderator;
    }

    public GuildMetaTable getGuildMetaTable() {
        return guildMetaTable;
    }

    public void setGuildMetaTable(GuildMetaTable guildMetaTable) {
        this.guildMetaTable = guildMetaTable;
    }
}
