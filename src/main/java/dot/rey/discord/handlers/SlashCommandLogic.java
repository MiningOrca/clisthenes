package dot.rey.discord.handlers;

import dot.rey.repository.ChannelUsersRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static dot.rey.discord.Utils.textChannelModeratorPermission;
import static dot.rey.discord.Utils.textChannelUserPermission;

@Component
public class SlashCommandLogic extends ListenerAdapter {

    @Autowired
    private ChannelUsersRepository channelUsersRepository;
    final static Logger logger = LoggerFactory.getLogger(SlashCommandLogic.class);



    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "leave_channel" -> retiredChannelUser(event.getChannel().asTextChannel(), event.getMember());
            case "ban_user" -> {
                logger.info("{} banned in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                banUserInTextChannel(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "add_moderator" -> {
                logger.info("{} set as moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                setAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "tolerate" -> {
                logger.info("{} forgive {} in channel {}", event.getMember(), event.getOption("name").getAsMember(), event.getChannel());
                forgiveUser(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "remove_moderator" -> {
                logger.info("{} removed from moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                removeAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
        }
    }

    private void removeAsModerator(TextChannel channel, Member member) {
        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelUserPermission, textChannelModeratorPermission).queue();
        channelUsersRepository.updateModerator(channel.getIdLong(), member.getIdLong(), false);
    }

    private void setAsModerator(TextChannel channel, Member member) {
        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelModeratorPermission, null).queue();
        logger.info(" Set user {} as moderator for channel {}", member, channel);
        channelUsersRepository.updateModerator(channel.getIdLong(), member.getIdLong(), true);
    }

    private void banUserInTextChannel(TextChannel newChannel, Member member) {
        channelUsersRepository.updateBanned(newChannel.getIdLong(), member.getIdLong(), true);
        channelUsersRepository.updateModerator(newChannel.getIdLong(), member.getIdLong(), false);
        retiredChannelUser(newChannel, member);
    }

    private void forgiveUser(TextChannel channel, Member member) {
        channelUsersRepository.updateBanned(channel.getIdLong(), member.getIdLong(), false);
    }

    private void retiredChannelUser(TextChannel newChannel, Member member) {
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, textChannelUserPermission).queue();
        logger.info("Retired view to user {} for channel {}", member, newChannel);
        if (!channelUsersRepository.isBanned(newChannel.getIdLong(), member.getIdLong()).orElse(false)) {
            channelUsersRepository.deleteByUserIdAndChannelId(member.getIdLong(), newChannel.getIdLong());
            logger.debug("User {} entre delete from DB for channel {}", member, newChannel);
        } else logger.info("Skip database deletion - User {} is banned in channel {}", member, newChannel);
    }
}
