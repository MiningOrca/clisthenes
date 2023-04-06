package dot.rey.discord.handlers;

import dot.rey.repository.BanRepository;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;

import static dot.rey.discord.Utils.*;
import static dot.rey.discord.Utils.textChannelUserPermission;
import static net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode;

@Component
@Transactional
public class ChannelSubscribeLogic extends ListenerAdapter {
    final static Logger logger = LoggerFactory.getLogger(ChannelSubscribeLogic.class);
    @Autowired
    private GuildMetaRepository metaRepository;
    @Autowired
    private BanRepository banRepository;
    @Autowired
    private ChannelUsersRepository channelUsersRepository;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var mentionedChannels = event.getMessage().getMentions().getChannels();
        if (!mentionedChannels.isEmpty()) {
            logger.info("Found channel mention in message with id" + event.getMessageId());
            if (channelUsersRepository.existsByChannelId(mentionedChannels.get(0).getIdLong())) {
                addReactionsOnChannelMessages(event.getMessage());
            }
        }
    }

    @Override
    public void onGenericMessageReaction(@NotNull GenericMessageReactionEvent event) {
        if (event instanceof MessageReactionAddEvent && Objects.requireNonNull(event.getMember()).getIdLong() != (event.getJDA().getSelfUser().getIdLong())) {
            if (event.getReaction().getEmoji().getAsReactionCode().equals(YES_REACTION)) {
                var channel = event.retrieveMessage().complete().getMentions().getChannels().get(0);
                if (!banRepository.existsByChannelIdAndUserId(channel.getIdLong(), event.getUserIdLong())) {
                    setUpChannelUser((TextChannel) channel, event.getMember());
                }
            } else if (event.getReaction().getEmoji().getAsReactionCode().equals(NO_REACTION)) {
                var channel = event.retrieveMessage().complete().getMentions().getChannels().get(0);
                retiredChannelUser((TextChannel) channel, event.getMember());
            }
            event.retrieveMessage().complete().removeReaction(event.getEmoji(), Objects.requireNonNull(event.getUser())).queue();
        }
    }

    private static void addReactionsOnChannelMessages(Message msg) {
        logger.info("Added reaction for message with channel mention {}", msg.getId());
        msg.addReaction(fromUnicode(YES_REACTION)).queue();
        msg.addReaction(fromUnicode(NO_REACTION)).queue();
    }

    private void setUpChannelUser(TextChannel newChannel, Member member) {
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelUserPermission, null).queue();
        var channel = new ChannelUsersTable();
        channel.setChannelId(newChannel.getIdLong());
        channel.setUserId(member.getIdLong());
        channel.setGuildMetaTable(metaRepository.findById(newChannel.getGuild().getIdLong()).orElseThrow(() -> new NoSuchElementException("Can't found proper guild in database")));
        channelUsersRepository.save(channel);
        logger.info("Granted view to user {} for channel {}", member, newChannel);
    }

    private void retiredChannelUser(TextChannel newChannel, Member member) {
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, textChannelUserPermission).queue();
        logger.info("Retired view to user {} for channel {}", member, newChannel);
        channelUsersRepository.deleteByUserIdAndChannelId(member.getIdLong(), newChannel.getIdLong());
    }
}
