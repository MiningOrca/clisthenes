package dot.rey.discord.handlers;

import dot.rey.discord.Utils;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.GuildMetaTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor;

@Component
public class GuildJoinLogic extends ListenerAdapter {

    final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);

    private final GuildMetaRepository guildMetaRepository;

    public GuildJoinLogic(GuildMetaRepository guildMetaRepository) {
        this.guildMetaRepository = guildMetaRepository;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        logger.info("Joined to guild " + event.getGuild());
        if (!guildMetaRepository.existsById(event.getGuild().getIdLong())) {
            logger.info("Can't find guild in database");
            setupSystemChannel(event);
            setupBanChannel(event);
        } else if (guildMetaRepository.getSystemChannelId(event.getGuild().getIdLong()) == null) {
            logger.info("System channel doesn't exists on database, created new one");
            setupSystemChannel(event);
        } else if (event.getGuild().getTextChannelById(guildMetaRepository.getSystemChannelId(event.getGuild().getIdLong())) == null) {
            logger.info("System channel exist on database but deleted from guild");
            setupSystemChannel(event);
        }
        if (guildMetaRepository.getBanChannelId(event.getGuild().getIdLong()) == null) {
            logger.info("Ban channel doesn't exists on database, created new one");
            setupBanChannel(event);
        } else if (event.getGuild().getTextChannelById(guildMetaRepository.getBanChannelId(event.getGuild().getIdLong())) == null) {
            logger.info("Ban channel exists in database but deleted from guild");
            setupBanChannel(event);
        }
        event.getGuild().updateCommands().addCommands(
                Commands.slash("ban_user", "Ban channel for a user")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("add_moderator", "Finds a random animal")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("tolerate", "Forgive user")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.MESSAGE_MANAGE)),
                Commands.slash("remove_moderator", "Forget random animal")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("leave_channel", "simply unsubscribe from channel")
                        .setDefaultPermissions(enabledFor(Permission.VIEW_CHANNEL))
        ).queue();
    }

    private void setupBanChannel(GuildJoinEvent event) {
        var channel = event.getGuild().createTextChannel(Utils.centralBanChannelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        logger.info("Created ban chanel");
        guildMetaRepository.updateBanChannel(event.getGuild().getIdLong(), channel.getIdLong());
    }

    private void setupSystemChannel(GuildJoinEvent event) {
        var channel = event.getGuild().createTextChannel(Utils.systemChannelName).complete();
        logger.info("Created system channel " + channel);
        var guildMeta = new GuildMetaTable();
        guildMeta.setGuildId(event.getGuild().getIdLong());
        guildMeta.setSystemChannelId(channel.getIdLong());
        guildMetaRepository.save(guildMeta);
    }
}
