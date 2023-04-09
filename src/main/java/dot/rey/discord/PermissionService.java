package dot.rey.discord;

import dot.rey.discord.handlers.SpecialChannelLogic;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static dot.rey.discord.Utils.Privilege.*;
import static dot.rey.discord.Utils.textChannelModeratorPermission;
import static dot.rey.discord.Utils.textChannelUserPermission;

@Service
public class PermissionService {

    final static Logger logger = LoggerFactory.getLogger(SpecialChannelLogic.class);
    @Autowired
    public GuildMetaRepository metaRepository;
    @Autowired
    public ChannelUsersRepository channelUsersRepository;

    public void setBasePermitsToUser(GuildChannel c, Member member) {
        logger.info("Granted view to user {} for channel {}", member, c);
        if (c instanceof TextChannel) {
            c.getPermissionContainer()
                    .upsertPermissionOverride(member)
                    .setPermissions(Utils.textChannelUserPermission, null).queue();
        } else if (c instanceof VoiceChannel) {
            c.getPermissionContainer()
                    .upsertPermissionOverride(member)
                    .setPermissions(Utils.voiceChannelUserPermission, null).queue();
        }
        var channelUser = channelUsersRepository
                .findByUserIdAndChannelId(member.getIdLong(), c.getIdLong())
                .orElseGet(ChannelUsersTable::new);
        channelUser.setGuildMetaTable(metaRepository.findById(c.getGuild().getIdLong()).orElseThrow());
        channelUser.setUserId(member.getIdLong());
        channelUser.setChannelId(c.getIdLong());
        channelUser.setPrivilege(USER.getOffset());
        channelUsersRepository.save(channelUser);
    }

    public void banChannelForMember(GuildChannel c, Member member) {
        logger.info("User {} banned in channel {}", member, c);
        c.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
        var channelUser = channelUsersRepository
                .findByUserIdAndChannelId(member.getIdLong(), c.getIdLong())
                .orElseGet(ChannelUsersTable::new);
        channelUser.setGuildMetaTable(metaRepository.findById(c.getGuild().getIdLong()).orElseThrow());
        channelUser.setUserId(member.getIdLong());
        channelUser.setChannelId(c.getIdLong());
        channelUser.setPrivilege(BAN.getOffset());
        channelUsersRepository.save(channelUser);
    }

    public void removeAsModerator(TextChannel channel, Member member) {
        logger.info("User {} removed moderator permits in channel {}", member, channel);
        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelUserPermission, textChannelModeratorPermission).queue();
        channelUsersRepository.updatePrivilege(channel.getIdLong(), member.getIdLong(), USER.getOffset());
    }

    public void setAsModerator(TextChannel channel, Member member) {
        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelModeratorPermission, null).queue();
        logger.info("Set user {} as moderator for channel {}", member, channel);
        channelUsersRepository.updatePrivilege(channel.getIdLong(), member.getIdLong(), MODERATOR.getOffset());
    }

    public void retiredChannelUser(TextChannel newChannel, Member member) {
        logger.info("Retired view to user {} for channel {}", member, newChannel);
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, textChannelUserPermission).queue();
        channelUsersRepository.deleteByUserIdAndChannelId(member.getIdLong(), newChannel.getIdLong());
    }
}
