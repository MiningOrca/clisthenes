package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.repository.UserChannelRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static dot.rey.discord.Utils.textChannelAdminPermission;

@Component
public class ChannelCreationLogic extends ListenerAdapter {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private UserChannelRepository userChannelRepository;
    private final GuildMetaRepository metaRepository;
    final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);


    public ChannelCreationLogic(GuildMetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.GUILD_PUBLIC_THREAD) return;
        if (metaRepository.existsBySystemChannelId(event.getChannel().getIdLong()) && event.getAuthor() != event.getJDA().getSelfUser()) {
            logger.info("Event create new channel triggered by " + event.getAuthor() + " for guild " + event.getGuild());
            buildNewChannel(event);
        }
    }

    private void buildNewChannel(MessageReceivedEvent event) {
        logger.info("Building new channel");
        var splitMsg = event.getMessage().getContentRaw().split("\n");
        var channelName = splitMsg[0];
        if (splitMsg.length < 2) {
            logger.info("Malformed message, need at least one \\n  " + event.getMessage().getContentRaw());
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " please use '\\n' as setup").queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (splitMsg[0].contains("<#")) {
            logger.info("Malformed message, should not hava channel mentioned  " + event.getMessage().getContentRaw());
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " please do not use another channel mentions as channel name, moron").queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (userChannelRepository.findAllByOwnerId(event.getAuthor().getIdLong()).map(List::size).orElse(0) >=
                metaRepository.getChannelLimitByGuildId(event.getGuild().getIdLong())) {
            logger.info("User {} reached limit with channel creation", event.getMember());
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " you're reached a limit with channel created").queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        }
        var newChannel = createNewChannel(channelName, event);
        replaceInitMessage(event, newChannel);
        permissionService.saveNewChannelInDB(event, newChannel.getIdLong());
        logger.info("Channel built successfully");
    }

    private void replaceInitMessage(MessageReceivedEvent event, TextChannel newChannel) {
        var message = MessageCreateBuilder.fromMessage(event.getMessage())
                .setContent(event.getMessage().getContentRaw().concat("\n").concat(newChannel.getAsMention())).build();
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(message).queue();
        logger.info("Replace init message");
    }

    private TextChannel createNewChannel(String channelName, MessageReceivedEvent event) {
        var newChannel = event.getGuild()
                .createTextChannel(channelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .addMemberPermissionOverride(event.getAuthor().getIdLong(), textChannelAdminPermission, null)
                .complete();
        logger.info("Channel created " + newChannel);
        return newChannel;
    }
}
