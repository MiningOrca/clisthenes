package dot.ray.discord;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DiscordClient implements CommandLineRunner {

    final DiscordSetup discordSetup;
    final DiscordChannelFilter discordChannelFilter;

    public DiscordClient(DiscordSetup discordSetup, DiscordChannelFilter discordChannelFilter) {
        this.discordSetup = discordSetup;
        this.discordChannelFilter = discordChannelFilter;
    }

    @Override
    public void run(String... args) throws Exception {
        var jda = JDABuilder
                .createDefault("MTAzNDg2NTUwNzQ0MDAxMzM3Mw.GWWcXh.i5Gq_WPF_A0iE06gu1uN2wHR-DxwoBV2k4TO5E") //Mmmm hardcoded
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                .build();
        jda.addEventListener(discordSetup);
        jda.addEventListener(discordChannelFilter);
    }
}
