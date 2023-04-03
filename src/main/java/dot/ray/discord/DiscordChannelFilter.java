package dot.ray.discord;

import dot.ray.repository.DiscordMetaRepository;
import dot.ray.repository.GuildChannelRepository;
import dot.ray.repository.UsersMetaRepository;
import dot.ray.table.ChannelMeta;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiscordChannelFilter extends ListenerAdapter {
    final Logger logger = LoggerFactory.getLogger(DiscordChannelFilter.class);

    final DiscordMetaRepository discordMeta;
    final UsersMetaRepository usersMeta;
    final GuildChannelRepository guildChannel;

    public DiscordChannelFilter(DiscordMetaRepository discordMeta, UsersMetaRepository usersMeta, GuildChannelRepository guildChannel) {
        this.discordMeta = discordMeta;
        this.usersMeta = usersMeta;
        this.guildChannel = guildChannel;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        guildChannel.findByChannelIdAndGuildId(event.getChannel().getIdLong(), event.getGuild().getIdLong())
                .ifPresent(m -> startExecutor(m, event.getMessage()));
    }

    private void startExecutor(ChannelMeta channelMeta, Message message) {
        logger.info("start executor");
    }
}
