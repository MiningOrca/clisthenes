package dot.rey.discord;

import dot.rey.discord.handlers.*;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
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

    public DiscordClient(ChannelCreationLogic channelCreationLogic) {
        this.channelCreationLogic = channelCreationLogic;
    }

    @Override
    public void run(String... args) throws Exception {
        var jda = JDABuilder
                .createDefault("MTA5MzE0ODgyMTIyMTAyMzgwNQ.GlM-SE.Jr_4LvoOsq8sd3ykAzFgx0vii-laJl2YvH3ySU") //Mmmm hardcoded
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .build();
        jda.addEventListener(guildJoinLogic, slashCommandLogic, channelCreationLogic, channelSubscribeLogic, specialChannelLogic);
    }
}