package dot.rey.discord.handlers;

import dot.rey.repository.GuildMetaRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChannelRenameLogic extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(ChannelRenameLogic.class);
    private final GuildMetaRepository metaRepository;

    public ChannelRenameLogic(GuildMetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

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
                            .queue(message -> editMessage(event, message),
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

    private void editMessage(MessageReceivedEvent event, Message message) {
        logger.info("Editing message " + message);
        var currentEmbed = message.getEmbeds().get(0);
        var newMessage = event.getMessage();
        if (newMessage.getContentRaw().startsWith("/change")) { //undocumented feature xd
            newMessage.getMentions().getChannels().stream().findFirst()
                    .ifPresent(m -> message
                            .editMessage(m.getName() + "\n" + m.getAsMention()).queue());
        } else {
            var newEmbed = new EmbedBuilder(currentEmbed)
                    .setDescription(newMessage.getContentRaw()).build();
            message.editMessageEmbeds(newEmbed).queue();
        }
        logger.info("Message edited");
    }
}
