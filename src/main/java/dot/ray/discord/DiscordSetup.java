package dot.ray.discord;

import dot.ray.repository.DiscordMetaRepository;
import dot.ray.repository.GuildChannelRepository;
import dot.ray.repository.UsersMetaRepository;
import dot.ray.table.ChannelMeta;
import dot.ray.table.GuildMeta;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.VIEW_CHANNEL;

@Component
public class DiscordSetup extends ListenerAdapter {
    final Logger logger = LoggerFactory.getLogger(DiscordSetup.class);
    final GuildChannelRepository guildChannel;
    final DiscordMetaRepository discordMeta;
    final UsersMetaRepository usersMeta;
    private final String systemChannel = "system-channel-vs38";

    public DiscordSetup(GuildChannelRepository guildChannel, DiscordMetaRepository discordMeta, UsersMetaRepository usersMeta) {
        this.guildChannel = guildChannel;
        this.discordMeta = discordMeta;
        this.usersMeta = usersMeta;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        var guild = event.getGuild();
        var guildId = guild.getIdLong();
        var channel = getSystemChannel(guild);
        logger.info("Joined into guild {} with id {}", guild.getName(), guildId);
        if (!discordMeta.existsById(guildId)) {
            logger.info("Guild {} not found into datasource", guildId);
            channel.sendMessage("Hello! Let's do a little setup.\nRemember it's a private channel with fill access to bot setUp, do not show it to everyone!").queue();
            createGuildEntity(guild);
        } else {
            logger.info("Guild {} found into datasource", guildId);
            channel.sendMessage("We will continue our service!").queue();
            sendManagedChannels(guild);
            sendModerators(guild);
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("admins")) {
            sendModerators(Objects.requireNonNull(event.getGuild()));
        } else if (event.getName().equals("channels")) {
            sendManagedChannels(Objects.requireNonNull(event.getGuild()));
        } else if (event.getName().equals("addChannel")) {
            var textChannel = Objects.requireNonNull(event.getOption("channelname")).getAsChannel();
            logger.info("received slash command to add moderated channel {} for guild {}", textChannel.getId(), event.getGuild());
            addModeratedChannel(textChannel);
        } else if (event.getName().equals("deleteChannel")) {
            var textChannel = Objects.requireNonNull(event.getOption("channelname")).getAsChannel();
            logger.info("received slash command to delete moderated channel {} for guild {}", textChannel.getId(), event.getGuild());
            guildChannel.deleteByChannelIdAndGuildId(textChannel.getIdLong(), textChannel.getGuild().getIdLong());
        }
    }

    private void addModeratedChannel(GuildChannelUnion textChannel) {
        var channel = new ChannelMeta();
        channel.setChannelId(textChannel.getIdLong());
        channel.setGuildId(textChannel.getGuild().getIdLong());
        guildChannel.save(channel);
        logger.info("added channel {} for guild {}", textChannel.getId(), textChannel.getGuild().getId());
    }

    @NotNull
    private TextChannel getSystemChannel(Guild guild) {
        var channelId = Optional.ofNullable(discordMeta.getGuildMetaByGuildId(guild.getIdLong())
                .orElseGet(GuildMeta::new).getSystemChannelId());
        if (channelId.isPresent()) {
            return Optional.ofNullable(guild.getChannelById(TextChannel.class, channelId.get()))
                    .orElseGet(() -> createSystemChannel(guild));
        }
        return createSystemChannel(guild);
    }

    private void sendManagedChannels(Guild guild) {
        logger.debug("requested list of managed channels for guild {}", guild.getId());
        var channel = getSystemChannel(guild);
        var managedChannels = discordMeta
                .getReferenceById(guild.getIdLong()).getModerateChannels()
                .stream()
                .map(c -> c = "<#" + c + ">").collect(Collectors.toList());
        logger.info("Guild {} list of managed channels {}", guild.getId(), managedChannels);
        channel.sendMessage("Managed channels " + managedChannels).queue();
    }

    private void sendModerators(Guild guild) {
        logger.debug("requested list of moderators for guild {}", guild.getId());
        var channel = getSystemChannel(guild);
        var moderators = discordMeta.getReferenceById(guild.getIdLong()).getAdmins().stream()
                .map(c -> c = "<@" + c + ">").collect(Collectors.toList());
        logger.info("Guild {} list of moderators {}", guild.getId(), moderators);
        channel.sendMessage("List of moderators " + moderators).queue();
    }

    private TextChannel createSystemChannel(Guild guild) {
        guild.createTextChannel(systemChannel)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(VIEW_CHANNEL))
                .queue();
        var channel = guild.getTextChannels().stream().peek(l -> logger.info(l.toString())).filter(c -> c.getName().equals(systemChannel))
                .findFirst().orElseThrow();
        setupGuildCommands(channel);
        logger.info("Created system channel in guild {}", guild.getId());
        return channel;
    }

    private void setupGuildCommands(TextChannel channel) {
        channel.getGuild().updateCommands().addCommands(
                        Commands.slash("admins", "provide list of admins"),
                        Commands.slash("channels", "provide list of moderated channels"),
//                Commands.slash("expelAdmin", "expel admin").addOption(OptionType.USER, "username", ""),
                        Commands.slash("addChannel", "add channel to moderate").addOption(OptionType.CHANNEL, "channelname", ""),
                        Commands.slash("deleteChannel", "add channel to moderate").addOption(OptionType.CHANNEL, "channelname", ""))
                .queue();
    }

    private void createGuildEntity(Guild guild) {
        var guildMeta = new GuildMeta();
        guildMeta.setGuildName(guild.getName());
        guildMeta.setGuildId(guild.getIdLong());
        guildMeta.setGuildToken(UUID.randomUUID().toString());
        discordMeta.save(guildMeta);
    }
}
