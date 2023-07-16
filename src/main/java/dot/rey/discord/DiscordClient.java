package dot.rey.discord;

import dot.rey.discord.handlers.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DiscordClient implements CommandLineRunner {

    private final ChannelCreationLogic channelCreationLogic;
    private final GuildJoinLogic guildJoinLogic;
    private final SlashCommandLogic slashCommandLogic;
    private final ChannelSubscribeLogic channelSubscribeLogic;
    private final SpecialChannelLogic specialChannelLogic;
    private final ChannelRenameLogic channelRenameLogic;
    @Value("${discord.token}")
    private String token;

    public DiscordClient(ChannelCreationLogic channelCreationLogic, GuildJoinLogic guildJoinLogic, SlashCommandLogic slashCommandLogic, ChannelSubscribeLogic channelSubscribeLogic, SpecialChannelLogic specialChannelLogic, ChannelRenameLogic channelRenameLogic) {
        this.channelCreationLogic = channelCreationLogic;
        this.guildJoinLogic = guildJoinLogic;
        this.slashCommandLogic = slashCommandLogic;
        this.channelSubscribeLogic = channelSubscribeLogic;
        this.specialChannelLogic = specialChannelLogic;
        this.channelRenameLogic = channelRenameLogic;
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
