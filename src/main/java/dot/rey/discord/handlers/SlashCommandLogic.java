package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.ChannelUsersRepository;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static dot.rey.discord.Utils.Privilege.UNBANNED;

@Component
public class SlashCommandLogic extends ListenerAdapter {

    private final PermissionService permissionService;
    private final ChannelUsersRepository channelUsersRepository;
    private final Logger logger = LoggerFactory.getLogger(SlashCommandLogic.class);

    public SlashCommandLogic(PermissionService permissionService, ChannelUsersRepository channelUsersRepository) {
        this.permissionService = permissionService;
        this.channelUsersRepository = channelUsersRepository;
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "leave_channel" -> {
                permissionService.retiredChannelUser(event.getChannel().asTextChannel(), event.getMember());
                event.reply("Leaving").setEphemeral(true).queue();
            }
            case "spy_user" -> {
                logger.info("User {} copy {} subscriptions", event.getUser(), event.getOption("name").getAsMember());
                permissionService.copyUserSubscriptions(event.getMember(), event.getOption("name").getAsMember());
                event.reply("copy subscriptions of " + event.getOption("name").getAsMember())
                        .setEphemeral(true).queue();
            }
//            case "create_subchannel" -> {
//                permissionService.establishSubchannel(event, event.getOption("name").getAsString());
//            }
            case "start_migration" -> {
                permissionService.migrateMainChannel(event);
                event.reply("Finished channels migration")
                        .setEphemeral(true).queue();
            }
            //todo add trusted owner mechanic
            //todo add channel deletion for admin
            case "ban_user" -> {
                logger.info("{} banned in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.banChannelForMember(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
                event.reply(event.getOption("name").getAsMember() + " banned")
                        .setEphemeral(true).queue();
            }
            case "add_moderator" -> {
                logger.info("{} set as moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.setAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
                event.reply(event.getOption("name").getAsMember() + " added as moderator")
                        .setEphemeral(true).queue();
            }
            case "tolerate" -> {
                logger.info("{} forgive {} in channel {}", event.getMember(), event.getOption("name").getAsMember(), event.getChannel());
                forgiveUser(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
                event.reply(event.getOption("name").getAsMember() + " unbanned")
                        .setEphemeral(true).queue();
            }
            case "remove_moderator" -> {
                logger.info("{} removed from moderator in channel {} by {}", event.getOption("name").getAsMember(), event.getChannel(), event.getMember());
                permissionService.removeAsModerator(event.getChannel().asTextChannel(), event.getOption("name").getAsMember());
                event.reply(event.getOption("name").getAsMember() + " retrieve moderator privileges")
                        .setEphemeral(true).queue();
            }
            case "clean_ownership" -> {
                logger.info("Guild admin {} clean ownership flag from {}", event.getMember(), event.getOption("name").getAsMember());
                permissionService.cleanChannelOwnership(event.getOption("name").getAsMember());
                event.reply(event.getOption("name").getAsMentionable().getAsMention() + " can now create one more channel")
                        .setEphemeral(true).queue();
            }
        }
    }

    private void forgiveUser(TextChannel channel, Member member) {
        channelUsersRepository.updatePrivilege(channel.getIdLong(), member.getIdLong(), UNBANNED.getOffset());
    }
}
