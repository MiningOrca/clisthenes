package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.discord.Utils;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static dot.rey.discord.Utils.Privilege.BAN;

@Component
public class SpecialChannelLogic extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(SpecialChannelLogic.class);

    private final PermissionService permissionService;
    private final GuildMetaRepository metaRepository;
    private final ChannelUsersRepository channelUsersRepository;

    public SpecialChannelLogic(PermissionService permissionService, GuildMetaRepository metaRepository, ChannelUsersRepository channelUsersRepository) {
        this.permissionService = permissionService;
        this.metaRepository = metaRepository;
        this.channelUsersRepository = channelUsersRepository;
    }

    @Override
    //why have it here? dunno
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        var userChannels = channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndUserId(event.getGuild().getIdLong(), event.getMember().getIdLong());
        userChannels.forEach(channelUsersTable -> setupChannel(channelUsersTable, event));
    }

    private void setupChannel(ChannelUsersTable channelUsersTable, GuildMemberJoinEvent event) {
        switch (Utils.Privilege.getFromOffset(channelUsersTable.getPrivilege())) {
            case BAN -> permissionService
                    .banChannelForMember(event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()), event.getMember());
            case USER -> permissionService
                    .setBasePermitsToUser(event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()), event.getMember());
            case MODERATOR -> permissionService
                    .setAsModerator(Objects.requireNonNull(event.getGuild().getTextChannelById(channelUsersTable.getChannelId())), event.getMember());
            case OWNER, CHOSEN_ADMIN -> logger.error("OWNER or CHOSEN_ADMIN should not be saved");
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() == metaRepository.getBanChannelId(event.getGuild().getIdLong())) {
            var rawMessage = event.getMessage().getContentRaw();
            if (rawMessage.startsWith("help")) {
                event.getChannel().sendMessage(Utils.helpText).queue();
            } else if (rawMessage.startsWith("ban")) {
                disableChannelForUser(event);
            } else if (rawMessage.startsWith("forgive")) {
                enableChannelForUser(event);
            } else if (rawMessage.startsWith("getbans")) {
                provideBanList(event);
            } else if (rawMessage.startsWith("spyuser")) {
                provideChannelList(event);
            }
        }
    }

    private void provideChannelList(MessageReceivedEvent event) {
        final StringBuilder message = new StringBuilder();
        var members = event.getMessage().getMentions().getMembers();
        members.forEach(m -> channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndUserId(event.getGuild().getIdLong(), m.getIdLong())
                .forEach(c -> buildUserSubscriptionsList(message, c)));
        event.getChannel().sendMessage(message.toString()).queue();
    }

    private void buildUserSubscriptionsList(StringBuilder message, ChannelUsersTable c) {
        message.append("<@")
                .append(c.getUserId()).append(">").append(" has ")
                .append(Utils.Privilege.getFromOffset(c.getPrivilege()).name().toLowerCase())
                .append(" privileges in channel <#")
                .append(c.getChannelId())
                .append(">").append(" subscribed from ").append(c.getSubscriptionDate()).append("\n");
    }

    private void provideBanList(MessageReceivedEvent event) {
        final StringBuilder message = new StringBuilder();
        channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndPrivilege(event.getGuild().getIdLong(), BAN.getOffset())
                .forEach(c -> message.append("<@").append(c.getUserId()).append(">")
                        .append("banned in <#").append(c.getChannelId()).append(">")
                        .append(" from ")
                        .append(c.getBannedDate()).append("\n"));
        event.getChannel().sendMessage(message.toString()).queue();
    }

    private void enableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> permissionService.setBasePermitsToUser(c, member)));
    }

    private void disableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> permissionService.banChannelForMember(c, member)));
    }

}
