package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.repository.UserChannelRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static dot.rey.discord.Utils.textChannelAdminPermission;
import static java.awt.Color.GRAY;

@Component
public class ChannelCreationLogic extends ListenerAdapter {

    private final PermissionService permissionService;
    private final UserChannelRepository userChannelRepository;
    private final GuildMetaRepository metaRepository;
    private final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);


    public ChannelCreationLogic(GuildMetaRepository metaRepository, PermissionService permissionService, UserChannelRepository userChannelRepository) {
        this.metaRepository = metaRepository;
        this.permissionService = permissionService;
        this.userChannelRepository = userChannelRepository;
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
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " please use '\\n' as setup")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (splitMsg.length > Message.MAX_CONTENT_LENGTH) {
            logger.info("Malformed message, too many symbols " + event.getMessage().getContentRaw());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " no more than " + Message.MAX_CONTENT_LENGTH + " symbols")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
        } else if (splitMsg[0].contains("<#")) {
            logger.info("Malformed message, should not hava channel mentioned  " + event.getMessage().getContentRaw());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " please do not use another channel mentions as channel name, moron")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (userChannelRepository.findAllByOwnerIdAndGuildMetaTable_GuildId(event.getAuthor().getIdLong(), event.getGuild().getIdLong()).size() >=
                metaRepository.getChannelLimitByGuildId(event.getGuild().getIdLong())) {
            logger.info("User {} reached limit with channel creation", event.getMember());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " you're reached a limit with channel created")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        }
        var newChannel = createNewChannel(channelName, event);
        replaceInitMessage(event, newChannel);
        permissionService.saveNewChannelInDB(event, newChannel.getIdLong());
        logger.info("Channel built successfully");
    }

    private void replaceInitMessage(MessageReceivedEvent event, TextChannel newChannel) {
        var message = new MessageCreateBuilder()
                .addEmbeds(buildEmbed(event, newChannel))
                .addContent(newChannel.getName() + "\n" + newChannel.getAsMention())
                .build();
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(message).queue();
        logger.info("Replace init message");
    }

    private MessageEmbed buildEmbed(MessageReceivedEvent event, TextChannel newChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
        embedBuilder.setColor(GRAY);
        embedBuilder.setTitle(newChannel.getName() + " " + newChannel.getAsMention());
        embedBuilder.setDescription(StringUtils.substringAfter(event.getMessage().getContentRaw(), "\n"));
        if (!event.getMessage().getAttachments().isEmpty()) {
            event.getMessage().getAttachments().stream().filter(Message.Attachment::isImage).findFirst()
                    .ifPresent(a -> embedBuilder.setThumbnail(a.getUrl()));
        }
        return embedBuilder.build();
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
