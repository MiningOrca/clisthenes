package dot.rey.discord.handlers;

import dot.rey.repository.GuildMetaRepository;
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
        if (msg.getType() == MessageType.THREAD_STARTER_MESSAGE || msg.getChannel().getType() == ChannelType.GUILD_PUBLIC_THREAD) {
            if (metaRepository.findByGuildId(event.getGuild().getIdLong()).getSystemChannelId() == msg.getChannel().asThreadChannel().getParentChannel().getIdLong()) {
                var parentId = msg.getGuildChannel().asThreadChannel().getId();
                event.getChannel().asThreadChannel()
                        .getParentChannel().asTextChannel().retrieveMessageById(1122103694066995280l + "")
                        .queue(message -> {
                                    logger.info("Editing message " + message);
                                    var channel = message.getMentions().getChannels().get(0).getAsMention();
                                    message.editMessage(event.getMessage().getContentRaw() + "\n" + channel).queue();
                                    logger.info("Message edited");
                                    event.getMessage().delete().queue();
                                },
                                error -> {
                                    logger.error("Failed to retrieve message with ID: " + parentId);
                                    error.printStackTrace();
                                });
            }
        }
    }
//
//    @Override
//    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
//        // Ignore messages sent by the bot itself
//        if (event.getAuthor().isBot()) {
//            return;
//        }
//
//        // Check if the message is in the special channel you want to handle
//        long specialChannelId = 1234567890L; // Replace with the actual ID of the special channel
//        if (event.getChannel().getIdLong() != specialChannelId) {
//            return;
//        }
//
//        // Check if the message is a thread message
//        Message message = event.getMessage();
//        if (!message.isFromThread()) {
//            return;
//        }
//
//        // Handle the thread message
//        System.out.println("Thread message received in #" + event.getChannel().getName() + ": " + message.getContentRaw());
//    }
}
