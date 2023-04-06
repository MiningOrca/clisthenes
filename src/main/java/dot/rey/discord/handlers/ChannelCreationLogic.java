package dot.rey.discord.handlers;

import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static dot.rey.discord.Utils.textChannelAdminPermission;

@Component
public class ChannelCreationLogic extends ListenerAdapter {

    @Autowired
    private ChannelUsersRepository channelUsersRepository;
    private final GuildMetaRepository metaRepository;
    final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);


    public ChannelCreationLogic(GuildMetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (metaRepository.existsBySystemChannelId(event.getChannel().getIdLong()) && event.getAuthor() != event.getJDA().getSelfUser()) {
            logger.info("Event create new channel triggered by " + event.getAuthor() + " for guild " + event.getGuild());
            buildNewChannel(event);
        }
    }

    private void buildNewChannel(MessageReceivedEvent event) {
        logger.info("Building new channel");
        var splitMsg = event.getMessage().getContentRaw().split("\n");
        if (splitMsg.length < 2) {
            logger.info("Malformed message, need at least one \\n  " + event.getMessage().getContentRaw());
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + " please use '\\n' as setup").queue();
        }
        var channelName = splitMsg[0];
        var newChannel = createNewChannel(channelName, event);
        replaceInitMessage(event, newChannel);
        saveNewChannelInDB(event, newChannel.getIdLong());
    }

    private void saveNewChannelInDB(MessageReceivedEvent event, long channelId) {
        var channel = new ChannelUsersTable();
        channel.setChannelId(channelId);
        channel.setUserId(event.getAuthor().getIdLong());
        channel.setModerator(true);
        channel.setOwner(true);
        channel.setGuildMetaTable(metaRepository.findById(event.getGuild().getIdLong()).orElseThrow(() -> new NoSuchElementException("Can't found proper guild in database")));
        channelUsersRepository.save(channel);
    }

    private void replaceInitMessage(MessageReceivedEvent event, TextChannel newChannel) {
        var message = MessageCreateBuilder.fromMessage(event.getMessage())
                .setContent(event.getMessage().getContentRaw().replaceFirst(newChannel.getName(), newChannel.getAsMention())).build();
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(message).queue();
    }

    private TextChannel createNewChannel(String channelName, MessageReceivedEvent event) {
        var newChannel = event.getGuild().createTextChannel(channelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .addMemberPermissionOverride(event.getAuthor().getIdLong(), textChannelAdminPermission, null)
                .complete();
        logger.info("Channel created " + newChannel);
        return newChannel;
    }
}
