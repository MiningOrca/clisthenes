package dot.rey.table;

import javax.persistence.*;

@Entity
@Table(name = "channel_users", uniqueConstraints = {@UniqueConstraint(columnNames = {"channel_id", "user_id"})})
public class ChannelUsersTable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "privilege")
    private Integer privilege = 0;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private GuildMetaTable guildMetaTable;

    @ManyToOne
    @JoinColumn(name = "channel_id", nullable = false)
    private ChannelsTable channelsTable;

    public Long getChannelId() {
        return channelsTable.getChannelId();
    }

    public ChannelsTable getUserChannelsTable() {
        return channelsTable;
    }

    public void setUserChannelsTable(ChannelsTable channelsTable) {
        this.channelsTable = channelsTable;
    }

    public void setChannelId(Long channelId) {
        channelsTable.setChannelId(channelId);
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
