package dot.rey.discord;

import dot.rey.discord.handlers.SpecialChannelLogic;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.repository.SubchannelRepository;
import dot.rey.repository.UserChannelRepository;
import dot.rey.table.ChannelUsersTable;
import dot.rey.table.ChannelsTable;
import dot.rey.table.GuildMetaTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dot.rey.discord.Utils.*;
import static dot.rey.discord.Utils.Privilege.*;

@Service
@Transactional
public class PermissionService {

    final static Logger logger = LoggerFactory.getLogger(SpecialChannelLogic.class);
    @Autowired
    private GuildMetaRepository metaRepository;
    @Autowired
    private SubchannelRepository subchannelRepository;
    @Autowired
    private ChannelUsersRepository channelUsersRepository;
    @Autowired
    private UserChannelRepository userChannelRepository;

    public void setBasePermitsToUser(GuildChannel c, Member member) {
        Set<GuildChannel> channelsSet = userChannelRepository.findById(c.getIdLong()).stream()
                .map(ChannelsTable::getParentChannelId)
                .filter(Objects::nonNull)
                .flatMap(p -> userChannelRepository.findAllByParentChannelId(p).stream())
                .map(id -> c.getGuild().getGuildChannelById(id))
                .collect(Collectors.toSet());
        if (channelsSet.isEmpty()) {
            subscribeUserToChannel(member, c);
        } else channelsSet.forEach(channel -> subscribeUserToChannel(member, channel));
    }

    public void setBasePermitsToUserWithCheck(Member member, GuildChannel channel) {
        var userPriv = channelUsersRepository.findByUserIdAndChannelsTable_ChannelId(member.getIdLong(), channel.getIdLong());
        if (member.getPermissions(channel).contains(Permission.VIEW_CHANNEL)) {
            logger.info("User {} try to subscribe already subscribed channel {}", member, channel);
        } else if (userPriv.isEmpty() || userPriv.get().getPrivilege() != Utils.Privilege.BAN.getOffset()) {
            this.setBasePermitsToUser(channel, member);
        }
    }

    private void subscribeUserToChannel(Member member, GuildChannel c) {
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
                .findByUserIdAndChannelsTable_ChannelId(member.getIdLong(), c.getIdLong())
                .orElseGet(ChannelUsersTable::new);
        var guildMetaTable = metaRepository.findById(c.getGuild().getIdLong()).orElseThrow();
        channelUser.setGuildMetaTable(guildMetaTable);
        channelUser.setUserChannelsTable(userChannelRepository.findById(c.getIdLong())
                .orElseGet(() -> userChannelRepository.save(new ChannelsTable(c.getIdLong(), guildMetaTable))));
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
                .findByUserIdAndChannelsTable_ChannelId(member.getIdLong(), c.getIdLong())
                .orElseGet(ChannelUsersTable::new);
        channelUser.setGuildMetaTable(metaRepository.findById(c.getGuild().getIdLong()).orElseThrow());
        channelUser.setUserId(member.getIdLong());
        channelUser.setChannelId(c.getIdLong());
        channelUser.setBannedDate(LocalDateTime.now());
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

    public void retiredChannelUser(GuildChannel newChannel, Member member) {
        logger.info("Retired view to user {} for channel {}", member, newChannel);
        newChannel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, textChannelUserPermission).queue();
        channelUsersRepository.deleteByUserIdAndChannelsTable_ChannelId(member.getIdLong(), newChannel.getIdLong());
    }

    public void copyUserSubscriptions(Member recipient, Member donor) {
        var recipientSubscribedChannels = channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndUserId(recipient.getGuild().getIdLong(), recipient.getIdLong())
                .stream().filter(c -> c.getPrivilege() != UNBANNED.offset)
                .map(ChannelUsersTable::getChannelId)
                .collect(Collectors.toSet());
        channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndUserId(donor.getGuild().getIdLong(), donor.getIdLong()).stream()
                //we do not care about performance, can't be more than 1k channels
                .filter(c -> c.getPrivilege() != BAN.offset || c.getPrivilege() != UNBANNED.offset)
                .map(ChannelUsersTable::getChannelId)
                .filter(id -> !recipientSubscribedChannels.contains(id))
                //but in other case this can take a lot of time, and I'm not sure how will it work from discord side
                .forEach(id -> subscribeUserToChannel(recipient, recipient.getGuild().getGuildChannelById(id)));
    }


    public void saveNewChannelInDB(MessageReceivedEvent event, long channelId) {
        insertToDB(event.getGuild().getIdLong(), event.getAuthor().getIdLong(), channelId);
    }

    private void insertToDB(long guildId, long authorId, long channelId) {
        logger.info("Create database entries for channel {}", channelId);
        GuildMetaTable guildMetaTable = metaRepository.findById(guildId).orElseThrow(() -> new NoSuchElementException("Can't found proper guild in database"));
        var userChannel = new ChannelsTable();
        userChannel.setGuildMetaTable(guildMetaTable);
        userChannel.setChannelId(channelId);
        userChannel.setOwnerId(authorId);
        userChannel = userChannelRepository.save(userChannel);

        var channel = new ChannelUsersTable();
        channel.setUserChannelsTable(userChannel);
        channel.setUserId(authorId);
        channel.setPrivilege(Privilege.OWNER.getOffset());
        channel.setGuildMetaTable(guildMetaTable);
        channelUsersRepository.save(channel);
    }

    public void establishSubchannel(SlashCommandInteractionEvent event, String channelName) {
        GuildMetaTable guildMetaTable = metaRepository.findById(event.getGuild().getIdLong())
                .orElseThrow(() -> new NoSuchElementException("Can't found proper guild in database"));

        TextChannel newChannel = event
                .getGuild()
                .createTextChannel(channelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .addMemberPermissionOverride(event.getMember().getIdLong(), textChannelAdminPermission, null)
                .complete();

        var userChannel = new ChannelsTable();
        userChannel.setGuildMetaTable(guildMetaTable);
        userChannel.setChannelId(newChannel.getIdLong());
        userChannel.setParentChannelId(event.getChannel().getIdLong());
        userChannelRepository.save(userChannel);

        event.getChannel().sendMessage("@everyone Established subchannel " +
                "<#" + newChannel.getIdLong() + "> for this channel. " +
                "Please press yes as reaction for this message.").queue();
    }

    public void migrateMainChannel(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var channel = event.getChannel();
        Objects.requireNonNull(guild).getChannels()
                .stream()
                .filter(c -> c.getType() == ChannelType.TEXT)
                .forEach(c -> {
                    insertToDB(guild.getIdLong(), 0L, c.getIdLong());
                    if (event.getOption("withPost") != null && event.getOption("withPost").getAsBoolean()) {
                        channel.sendMessage("Subscribe to ".concat(c.getName()).concat("\n").concat(c.getAsMention())).queue();
                    }
                });
        GuildMetaTable guildMetaTable = metaRepository.findById(guild.getIdLong()).orElseThrow(() -> new NoSuchElementException("Can't found proper guild in database"));
        guildMetaTable.setSystemChannelId(channel.getIdLong());
        metaRepository.save(guildMetaTable);
    }

    public void cleanChannelOwnership(Member name) {
        userChannelRepository.findAllByOwnerIdAndGuildMetaTable_GuildId(name.getIdLong(), name.getGuild().getIdLong())
                .stream().findFirst().ifPresent(channel -> {
                    channel.setOwnerId(0L);
                    userChannelRepository.save(channel);
                });
    }
}
