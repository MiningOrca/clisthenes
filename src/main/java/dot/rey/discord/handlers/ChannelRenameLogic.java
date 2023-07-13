package dot.rey.discord.handlers;

import dot.rey.repository.GuildMetaRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelRenameLogic extends ListenerAdapter {

    final Logger logger = LoggerFactory.getLogger(ChannelRenameLogic.class);


    @Autowired
    private GuildMetaRepository metaRepository;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var msg = event.getMessage();
        var member = event.getMember();
        if (member != null && member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            if (msg.getType() == MessageType.THREAD_STARTER_MESSAGE || msg.getChannel().getType() == ChannelType.GUILD_PUBLIC_THREAD) {
                if (metaRepository.findByGuildId(event.getGuild().getIdLong()).getSystemChannelId() == msg.getChannel().asThreadChannel().getParentChannel().getIdLong()) {
                    var parentId = msg.getGuildChannel().asThreadChannel().getId();
                    event.getChannel().asThreadChannel()
                            .getParentChannel().asTextChannel().retrieveMessageById(parentId)
                            .queue(message -> {
                                        logger.info("Editing message " + message);
                                        var channel = message.getMentions().getChannels().get(0).getAsMention();
                                        message.editMessage(event.getMessage().getContentRaw() + "\n" + channel).queue();
                                        logger.info("Message edited");
                                    },
                                    error -> {
                                        logger.error("Failed to retrieve message with ID: " + parentId);
                                        error.printStackTrace();
                                    });
                    try {
                        event.getMessage().delete().complete();
                    } catch (IllegalStateException e) {
                        logger.info("State exception of discord {}", e.getMessage());
                    }
                }
            }
        }
    }
}
