package dot.rey.table;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guild_metadata")
public class GuildMetaTable {

    @Id
    @Column(name = "guild_id", nullable = false, unique = true)
    private Long guildId;

    @Column(name = "system_channel")
    private Long systemChannelId;

    @Column(name = "tr21_channel")
    private Long tr21ChannelId;

    @Column(name = "user_channel_limit")
    private Long channelLimit = 3L;

    @OneToMany(mappedBy = "guildMetaTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelUsersTable> channels = new ArrayList<>();

    @OneToMany(mappedBy = "guildMetaTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelsTable> channelsTables = new ArrayList<>();

    public Long getChannelLimit() {
        return channelLimit;
    }

    public void setChannelLimit(Long channelLimit) {
        this.channelLimit = channelLimit;
    }

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long guildId) {
        this.guildId = guildId;
    }

    public Long getSystemChannelId() {
        return systemChannelId;
    }

    public void setSystemChannelId(Long systemChannelId) {
        this.systemChannelId = systemChannelId;
    }

    public Long getTr21ChannelId() {
        return tr21ChannelId;
    }

    public void setTr21ChannelId(Long tr21ChannelId) {
        this.tr21ChannelId = tr21ChannelId;
    }

    public List<ChannelUsersTable> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelUsersTable> channels) {
        this.channels = channels;
    }
}
