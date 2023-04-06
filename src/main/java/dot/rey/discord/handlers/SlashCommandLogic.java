package dot.rey.discord.handlers;

import dot.rey.repository.BanRepository;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.BanTable;
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
    private BanRepository banRepository;
    @Autowired
    private GuildMetaRepository metaRepository;
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
        retiredChannelUser(newChannel, member);
        banRepository.save(new BanTable(newChannel.getIdLong(), member.getIdLong(), metaRepository.findByGuildId(newChannel.getGuild().getIdLong())));
        channelUsersRepository.deleteByUserIdAndChannelId(member.getIdLong(), newChannel.getIdLong());
    }

    private void forgiveUser(TextChannel channel, Member member) {
        banRepository.deleteByChannelIdAndUserId(channel.getIdLong(), member.getIdLong());
    }

    private void retiredChannelUser(TextChannel newChannel, Member member) {
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, textChannelUserPermission).queue();
        logger.info("Retired view to user {} for channel {}", member, newChannel);
        channelUsersRepository.deleteByUserIdAndChannelId(member.getIdLong(), newChannel.getIdLong());
    }
}
