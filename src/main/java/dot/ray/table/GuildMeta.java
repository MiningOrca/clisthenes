package dot.ray.table;


import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "guild_meta")
public class GuildMeta {
    @Id
    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "system_channel_id", nullable = false)
    private String systemChannelId;

    @Column(name = "guild_name", nullable = false)
    private String guildName;

    @Column(name = "guild_token", nullable = false)
    private String guildToken;

    @Column(name = "list_channel")
    @ElementCollection
    private List<String> moderateChannels;

    @Column(name = "list_admins")
    @ElementCollection
    private List<String> admins;

    public Long getGuildId() {
        return guildId;
    }

    public void setGuildId(Long id) {
        this.guildId = id;
    }

    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    public String getGuildToken() {
        return guildToken;
    }

    public void setGuildToken(String guildToken) {
        this.guildToken = guildToken;
    }

    public List<String> getModerateChannels() {
        return moderateChannels;
    }

    public void setModerateChannels(List<String> moderateChannels) {
        this.moderateChannels = moderateChannels;
    }

    public List<String> getAdmins() {
        return admins;
    }

    public void setAdmins(List<String> admins) {
        this.admins = admins;
    }

    public String getSystemChannelId() {
        return systemChannelId;
    }

    public void setSystemChannelId(String systemChannelId) {
        this.systemChannelId = systemChannelId;
    }
}
