package dot.rey.discord.handlers;

import dot.rey.discord.Utils;
import dot.rey.repository.ChannelUsersRepository;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.ChannelUsersTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static dot.rey.discord.Utils.textChannelModeratorPermission;

@Component
public class SpecialChannelLogic extends ListenerAdapter {

    final static Logger logger = LoggerFactory.getLogger(SpecialChannelLogic.class);
    @Autowired
    private GuildMetaRepository metaRepository;

    @Autowired
    private ChannelUsersRepository channelUsersRepository;

    @Override
    //why have it here? dunno
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        var userChannels = channelUsersRepository.findAllByGuildMetaTable_GuildIdAndUserId(event.getGuild().getIdLong(), event.getMember().getIdLong());
        userChannels.forEach(channelUsersTable -> setupChannel(channelUsersTable, event));
    }

    private void setupChannel(ChannelUsersTable channelUsersTable, GuildMemberJoinEvent event) {
        if (channelUsersTable.isBanned()) {
            takeVisionFromUser(event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()), event.getMember());
        } else if (channelUsersTable.isModerator()) {
            setAsModerator(event.getGuild().getTextChannelById(channelUsersTable.getChannelId()), event.getMember());
        } else {
            takeVisionFromUser(event.getGuild().getGuildChannelById(channelUsersTable.getChannelId()), event.getMember());
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
            }
        }
    }

    private void provideBanList(MessageReceivedEvent event) {
        //ohohoho you should kill me for this one. But it so much fun
        final StringBuilder message = new StringBuilder();
        metaRepository.findById(event.getGuild().getIdLong())
                .ifPresent(table -> table.getChannels().forEach(c -> {
                    if (c.isBanned())
                        message.append("<@").append(c.getUserId()).append(">").append("banned in <#").append(c.getChannelId()).append(">").append("\n");
                }));
        event.getChannel().sendMessage(message.toString()).queue();
    }

    private void enableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> takeVisionToUser(c, member)));
    }

    private void takeVisionToUser(GuildChannel c, Member member) {
        logger.info("User {} forgiven in channel {}", member, c);
        if (c instanceof TextChannel) {
            c.getPermissionContainer()
                    .upsertPermissionOverride(member)
                    .setPermissions(Utils.textChannelUserPermission, null).queue();
        } else if (c instanceof VoiceChannel) {
            c.getPermissionContainer()
                    .upsertPermissionOverride(member)
                    .setPermissions(Utils.voiceChannelUserPermission, null).queue();
        }
        channelUsersRepository.updateBanned(c.getIdLong(), member.getIdLong(), false);
    }


    private void disableChannelForUser(MessageReceivedEvent event) {
        var channels = event.getMessage().getMentions().getChannels();
        var members = event.getMessage().getMentions().getMembers();
        channels.forEach(c -> members.forEach(member -> takeVisionFromUser(c, member)));
    }

    private void takeVisionFromUser(GuildChannel c, Member member) {
        logger.info("User {} banned in channel {}", member, c);
        c.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
        var t = new ChannelUsersTable();//todo fixMe
        t.setGuildMetaTable(metaRepository.findById(c.getGuild().getIdLong()).orElseThrow());
        t.setUserId(member.getIdLong());
        t.setChannelId(c.getIdLong());
        t.setBanned(true);
        t.setModerator(false);
        channelUsersRepository.save(t);
    }

    private void setAsModerator(TextChannel channel, Member member) {
        channel.getPermissionContainer()
                .upsertPermissionOverride(member)
                .setPermissions(textChannelModeratorPermission, null).queue();
        logger.info(" Set user {} as moderator for channel {}", member, channel);
        channelUsersRepository.updateModerator(channel.getIdLong(), member.getIdLong(), true);
    }
}
