package dot.rey.discord;

import dot.rey.discord.handlers.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DiscordClient implements CommandLineRunner {

    final ChannelCreationLogic channelCreationLogic;
    @Autowired
    private GuildJoinLogic guildJoinLogic;
    @Autowired
    private SlashCommandLogic slashCommandLogic;
    @Autowired
    private ChannelSubscribeLogic channelSubscribeLogic;
    @Autowired
    private SpecialChannelLogic specialChannelLogic;
    @Autowired
    private ChannelRenameLogic channelRenameLogic;
    @Value("${discord.token}")
    private String token;

    public DiscordClient(ChannelCreationLogic channelCreationLogic) {
        this.channelCreationLogic = channelCreationLogic;
    }

    @Override
    public void run(String... args) {
        var jda = JDABuilder
                .createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .build();
        jda.addEventListener(guildJoinLogic, slashCommandLogic, channelCreationLogic, channelSubscribeLogic, specialChannelLogic, channelRenameLogic);
    }
}
