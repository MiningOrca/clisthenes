package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.discord.Utils;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Stack;

import static dot.rey.discord.Utils.Privilege.BAN;

@Component
public class SpecialChannelLogic extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(SpecialChannelLogic.class);

    private final PermissionService permissionService;
    private final GuildMetaRepository metaRepository;
    private final ChannelUsersRepository channelUsersRepository;
    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
                    .banChannelForMember(event.getMember(), event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()));
            case USER -> permissionService
                    .setBasePermitsToUser(event.getMember(), event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()));
            case MODERATOR -> permissionService
                    .setAsModerator(event.getMember(), Objects.requireNonNull(event.getGuild().getTextChannelById(channelUsersTable.getChannelId())));
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
        final var lines = new Stack<String>();
        var members = event.getMessage().getMentions().getMembers();
        members.forEach(m -> channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndUserId(event.getGuild().getIdLong(), m.getIdLong())
                .stream().map(this::buildUserSubscriptionsString)
                .forEach(lines::push));
        messageSendLogic(event, lines);
    }

    private void messageSendLogic(MessageReceivedEvent event, Stack<String> lines) {
        if (!lines.isEmpty()) {
            var message = new StringBuilder();
            while (!lines.isEmpty()) {
                String line = lines.pop();
                if (message.length() + line.length() + 1 >= Message.MAX_CONTENT_LENGTH) {
                    event.getChannel().sendMessage(message.toString()).queue();
                    message.setLength(0);
                }
                message.append(line).append("\n");
            }
            if (!message.isEmpty()) {
                event.getChannel().sendMessage(message.toString()).queue();
            }
        } else {
            event.getMessage().reply("No data in database for this user").queue();
        }
    }

    private String buildUserSubscriptionsString(ChannelUsersTable c) {
        return "<@" + c.getUserId() + "> has " +
                Utils.Privilege.getFromOffset(c.getPrivilege()).name().toLowerCase() +
                " privileges in channel " +
                "<#" + c.getChannelId() + "> from " + c.getSubscriptionDate().format(TIME_FORMAT);
    }

    private void provideBanList(MessageReceivedEvent event) {
        final var lines = new Stack<String>();
        channelUsersRepository
                .findAllByGuildMetaTable_GuildIdAndPrivilege(event.getGuild().getIdLong(), BAN.getOffset())
                .stream().map(this::buildBanMessage)
                .forEach(lines::push);
        messageSendLogic(event, lines);
    }

    private String buildBanMessage(ChannelUsersTable c) {

        return "<@" + c.getUserId() + ">" +
                "banned in <#" + c.getChannelId() + ">" +
                " from " + c.getBannedDate().format(TIME_FORMAT);
    }

    private void enableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> permissionService.setBasePermitsToUser(member, c)));
    }

    private void disableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> permissionService.banChannelForMember(member, c)));
    }

}
