package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.discord.Utils;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.table.GuildMetaTable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static dot.rey.discord.Utils.NO_REACTION;
import static dot.rey.discord.Utils.YES_REACTION;
import static net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor;

@Component
public class GuildJoinLogic extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);

    private final PermissionService permissionService;
    private final GuildMetaRepository guildMetaRepository;

    public GuildJoinLogic(GuildMetaRepository guildMetaRepository, PermissionService permissionService) {
        this.guildMetaRepository = guildMetaRepository;
        this.permissionService = permissionService;
    }


    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        setupGuild(event);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        logger.info("Joined to guild " + event.getGuild());
        setupGuild(event);
    }

    private void setupGuild(GenericGuildEvent event) {
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
                        .setDefaultPermissions(enabledFor(Permission.VIEW_CHANNEL)),
                Commands.slash("spy_user", "copy user subscription")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.VIEW_CHANNEL)),
                Commands.slash("create_channel", "channel creation procedure")
                        .addOption(OptionType.ATTACHMENT, "picture", "picture for you channel", false)
                        .setDefaultPermissions(enabledFor(Permission.VIEW_CHANNEL)),
//                Commands.slash("create_subchannel", "establish a group of channels based on this one")
//                        .addOption(OptionType.STRING, "name", "new channel name", true)
//                        .setDefaultPermissions(enabledFor(Permission.MANAGE_CHANNEL)),
                Commands.slash("start_migration", "migrate all current guild channels as msg with join possibility")
                        .addOption(OptionType.BOOLEAN, "withposting", "post messages to join into migrated channel", false)
                        .setDefaultPermissions(enabledFor(Permission.ADMINISTRATOR)),
                Commands.slash("clean_ownership", "migrate all current guild channels as msg with join possibility")
                        .addOption(OptionType.USER, "name", "username", true)
                        .setDefaultPermissions(enabledFor(Permission.ADMINISTRATOR))
        ).queue();
        checkSystemChannel(event);
    }

    private void checkSystemChannel(GenericGuildEvent event) {
        var guild = event.getGuild();
        logger.info("Check system channel in guild {} {} after downtime", guild.getName(), guild.getId());
        var systemChannel = guild.getTextChannelById(guildMetaRepository.getSystemChannelId(event.getGuild().getIdLong()));
        try {
            Objects.requireNonNull(systemChannel).getIterableHistory().takeAsync(1000)
                    .thenAccept(messages -> messages.stream()
                            .filter(message -> !message.getMentions().getChannels().isEmpty())
                            .filter(message -> !message.getReactions().isEmpty())
                            .forEach(message -> {
                                Optional.ofNullable(message.getReaction(Emoji.fromUnicode(YES_REACTION)))
                                        .ifPresent(reaction ->
                                                processReactions(message, reaction, permissionService::setBasePermitsToUserWithCheck));
                                Optional.ofNullable(message.getReaction(Emoji.fromUnicode(NO_REACTION)))
                                        .ifPresent(reaction ->
                                                processReactions(message, reaction, permissionService::retiredChannelUserWithCheck));
                            }));
        } catch (Exception e) {
            logger.error("Exception during reading system channel messages in {} {}", guild.getName(), guild.getId());
        }
        logger.info("Check system channel in guild {} {} after downtime - finished", guild.getName(), guild.getId());
    }

    private void processReactions(Message message, MessageReaction reaction, BiConsumer<Member, GuildChannel> permissionOverride) {
        reaction
                .retrieveUsers().complete().stream()
                .filter(u -> !u.isBot())
                .map(user -> reaction.getGuild().retrieveMember(user).complete())
                .filter(Objects::nonNull)
                .forEach(member -> {
                    logger.info("Found reaction {} for member {} on message {}", reaction, member, message);
                    permissionOverride.accept(member, message.getMentions().getChannels().get(0));
                    reaction.removeReaction(member.getUser()).complete();
                });
    }

    private void setupBanChannel(GenericGuildEvent event) {
        var channel = event.getGuild().createTextChannel(Utils.centralBanChannelName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        logger.info("Created ban chanel");
        guildMetaRepository.updateBanChannel(event.getGuild().getIdLong(), channel.getIdLong());
    }

    private void setupSystemChannel(GenericGuildEvent event) {
        var channel = event.getGuild().createTextChannel(Utils.systemChannelName).complete();
        logger.info("Created system channel " + channel);
        var guildMeta = new GuildMetaTable();
        guildMeta.setGuildId(event.getGuild().getIdLong());
        guildMeta.setSystemChannelId(channel.getIdLong());
        guildMetaRepository.save(guildMeta);
    }
}
