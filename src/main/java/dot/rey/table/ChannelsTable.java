package dot.rey.table;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "channel_metadata")
public class ChannelsTable {
    @Id
    @Column(name = "channel_id", nullable = false, unique = true)
    private Long channelId;

    @Column(name = "owner_id")
    private Long ownerId = 0L;

    @Column(name = "parent_id")
    private Long parentChannelId;

    @OneToMany(mappedBy = "channelsTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelUsersTable> channels = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private GuildMetaTable guildMetaTable;

    public ChannelsTable() {
    }

    public ChannelsTable(Long channelId, GuildMetaTable guildMetaTable) {
        this.channelId = channelId;
        this.guildMetaTable = guildMetaTable;
    }

    public Long getParentChannelId() {
        return parentChannelId;
    }

    public void setParentChannelId(Long parentChannelId) {
        this.parentChannelId = parentChannelId;
    }

    public List<ChannelUsersTable> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelUsersTable> channels) {
        this.channels = channels;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public GuildMetaTable getGuildMetaTable() {
        return guildMetaTable;
    }

    public void setGuildMetaTable(GuildMetaTable guildMetaTable) {
        this.guildMetaTable = guildMetaTable;
    }
}
