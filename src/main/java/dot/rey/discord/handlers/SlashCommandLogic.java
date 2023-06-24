package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.ChannelUsersRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static dot.rey.discord.Utils.Privilege.*;

@Component
public class SlashCommandLogic extends ListenerAdapter {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private ChannelUsersRepository channelUsersRepository;
    final static Logger logger = LoggerFactory.getLogger(SlashCommandLogic.class);


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "leave_channel" ->
                    permissionService.retiredChannelUser(event.getChannel().asTextChannel(), event.getMember());
            case "spy_user" -> {
                logger.info("User {} copy {} subscriptions", event.getUser(), event.getOption("name").getAsMember());
                permissionService.copyUserSubscriptions(event.getMember(), event.getOption("name").getAsMember());
            }
//            case "create_subchannel" -> {
//                permissionService.establishSubchannel(event, event.getOption("name").getAsString());
//            }
            case "start_migration" -> {
                permissionService.migrateMainChannel(event);
            }
            //todo add trusted owner mechanic
            //todo add channel deletion for admin
            case "ban_user" -> {
                logger.info("{} banned in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.banChannelForMember(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "add_moderator" -> {
                logger.info("{} set as moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.setAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "tolerate" -> {
                logger.info("{} forgive {} in channel {}", event.getMember(), event.getOption("name").getAsMember(), event.getChannel());
                forgiveUser(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
            case "remove_moderator" -> {
                logger.info("{} removed from moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.removeAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
            }
        }
    }

    private void forgiveUser(TextChannel channel, Member member) {
        channelUsersRepository.updatePrivilege(channel.getIdLong(), member.getIdLong(), UNBANNED.getOffset());
    }
}
