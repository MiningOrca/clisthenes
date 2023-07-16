package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static dot.rey.discord.Utils.NO_REACTION;
import static dot.rey.discord.Utils.YES_REACTION;
import static net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode;

@Component
@Transactional
public class ChannelSubscribeLogic extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ChannelSubscribeLogic.class);
    private final PermissionService permissionService;
    private final ChannelUsersRepository channelUsersRepository;
    private final GuildMetaRepository metaRepository;

    public ChannelSubscribeLogic(PermissionService permissionService, ChannelUsersRepository channelUsersRepository, GuildMetaRepository metaRepository) {
        this.permissionService = permissionService;
        this.channelUsersRepository = channelUsersRepository;
        this.metaRepository = metaRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var mentionedChannels = event.getMessage().getMentions().getChannels();
        if (!mentionedChannels.isEmpty()) {
            logger.info("Found channel mention in message with id" + event.getMessageId());
            if (channelUsersRepository.existsByChannelsTable_ChannelId(mentionedChannels.get(0).getIdLong())
                    && event.getChannel().getIdLong() != metaRepository.getBanChannelId(event.getGuild().getIdLong())) {
                addReactionsOnChannelMessages(event.getMessage());
            }
        }
    }

    @Override
    public void onGenericMessageReaction(@NotNull GenericMessageReactionEvent event) {
        if (event instanceof MessageReactionAddEvent && Objects.requireNonNull(event.getMember()).getIdLong() != (event.getJDA().getSelfUser().getIdLong())) {
            if (event.getReaction().getEmoji().getAsReactionCode().equals(YES_REACTION)) {
                var channel = event.retrieveMessage().complete().getMentions().getChannels().get(0);
                permissionService.setBasePermitsToUserWithCheck(event.getMember(), channel);
                event.retrieveMessage().complete().removeReaction(event.getEmoji(), Objects.requireNonNull(event.getUser())).queue();
            } else if (event.getReaction().getEmoji().getAsReactionCode().equals(NO_REACTION)) {
                var channel = event.retrieveMessage().complete().getMentions().getChannels().get(0);
                permissionService.retiredChannelUser(channel, event.getMember());
                event.retrieveMessage().complete().removeReaction(event.getEmoji(), Objects.requireNonNull(event.getUser())).queue();
            }
        }
    }

    private void addReactionsOnChannelMessages(Message msg) {
        logger.info("Added reaction for message with channel mention {}", msg.getId());
        msg.addReaction(fromUnicode(YES_REACTION)).queue();
        msg.addReaction(fromUnicode(NO_REACTION)).queue();
    }
}
