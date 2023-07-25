package dot.rey.discord.handlers;

import dot.rey.discord.PermissionService;
import dot.rey.repository.GuildMetaRepository;
import dot.rey.repository.UserChannelRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static dot.rey.discord.Utils.textChannelAdminPermission;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;

@Component
public class ChannelCreationLogic extends ListenerAdapter {

    private final PermissionService permissionService;
    private final UserChannelRepository userChannelRepository;
    private final GuildMetaRepository metaRepository;
    private final Logger logger = LoggerFactory.getLogger(ChannelCreationLogic.class);


    public ChannelCreationLogic(GuildMetaRepository metaRepository, PermissionService permissionService, UserChannelRepository userChannelRepository) {
        this.metaRepository = metaRepository;
        this.permissionService = permissionService;
        this.userChannelRepository = userChannelRepository;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.GUILD_PUBLIC_THREAD) return;
        if (metaRepository.existsBySystemChannelId(event.getChannel().getIdLong()) && event.getAuthor() != event.getJDA().getSelfUser()) {
            logger.info("Event create new channel triggered by " + event.getAuthor() + " for guild " + event.getGuild());
            buildNewChannel(event);
        }
    }

    private void buildNewChannel(MessageReceivedEvent event) {
        logger.info("Building new channel");
        var splitMsg = event.getMessage().getContentRaw().split("\n");
        var channelName = splitMsg[0];
        if (splitMsg.length < 2) {
            logger.info("Malformed message, need at least one \\n  " + event.getMessage().getContentRaw());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " please use '\\n' as setup")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (splitMsg.length > Message.MAX_CONTENT_LENGTH) {
            logger.info("Malformed message, too many symbols " + event.getMessage().getContentRaw());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " no more than " + Message.MAX_CONTENT_LENGTH + " symbols")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
        } else if (splitMsg[0].contains("<#")) {
            logger.info("Malformed message, should not hava channel mentioned  " + event.getMessage().getContentRaw());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " please do not use another channel mentions as channel name, moron")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        } else if (userChannelRepository.findAllByOwnerIdAndGuildMetaTable_GuildId(event.getAuthor().getIdLong(), event.getGuild().getIdLong()).size() >=
                metaRepository.getChannelLimitByGuildId(event.getGuild().getIdLong())) {
            logger.info("User {} reached limit with channel creation", event.getMember());
            event.getChannel()
                    .sendMessage(event.getAuthor().getAsMention() + " you're reached a limit with channel created")
                    .queue(m -> m.delete().queueAfter(100, TimeUnit.SECONDS));
            event.getMessage().delete().queue();
            return;
        }
        var newChannel = createNewChannel(channelName, event.getGuild(), event.getAuthor());
        replaceInitMessage(event, newChannel);
        permissionService.saveNewChannelInDB(event, newChannel.getIdLong());
        logger.info("Channel built successfully");
    }

    private void replaceInitMessage(MessageReceivedEvent event, TextChannel newChannel) {
        var message = new MessageCreateBuilder()
                .addEmbeds(buildEmbedFromMessage(event, newChannel))
                .addContent(newChannel.getName() + "\n" + newChannel.getAsMention())
                .build();
        event.getMessage().delete().queue();
        event.getChannel().sendMessage(message).queue();
        logger.info("Replace init message");
    }

    private MessageEmbed buildEmbedFromMessage(MessageReceivedEvent event, TextChannel newChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl());
        embedBuilder.setColor(GRAY);
        embedBuilder.setTitle(newChannel.getName() + " " + newChannel.getAsMention());
        embedBuilder.setDescription(StringUtils.substringAfter(event.getMessage().getContentRaw(), "\n"));
        if (!event.getMessage().getAttachments().isEmpty()) {
            event.getMessage().getAttachments().stream().filter(Message.Attachment::isImage).findFirst()
                    .ifPresent(a -> embedBuilder.setThumbnail(a.getUrl()));
        }
        return embedBuilder.build();
    }

    private TextChannel createNewChannel(String channelName, Guild guild, User author) {
        var newChannel = guild
                .createTextChannel(channelName)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .addMemberPermissionOverride(author.getIdLong(), textChannelAdminPermission, null)
                .complete();
        logger.info("Channel created " + newChannel);
        return newChannel;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getInteraction().getModalId().equals("creationModal")) {
            logger.info("Get modal reply from {} in {}", event.getUser(), event.getChannel());
            if (userChannelRepository.findAllByOwnerIdAndGuildMetaTable_GuildId(event.getUser().getIdLong(), event.getGuild().getIdLong()).size() >=
                    metaRepository.getChannelLimitByGuildId(event.getGuild().getIdLong())) {
                logger.info("User {} reached limit with channel creation", event.getMember());
                event.reply(event.getUser().getAsMention() + " you're reached a limit with channel created").setEphemeral(true)
                        .queue();
                return;
            }
            var newChannel = createNewChannel(Objects.requireNonNull(event.getInteraction().getValue("channelName")).getAsString(),
                    event.getGuild(),
                    event.getUser());
            permissionService.insertNewChannelToDB(event.getGuild().getIdLong(), event.getUser().getIdLong(), newChannel.getIdLong());
            EmbedBuilder embedBuilder = buildModalEmbedResponse(event, newChannel);
            var message = new MessageCreateBuilder()
                    .addEmbeds(embedBuilder.build())
                    .addContent(newChannel.getName() + "\n" + newChannel.getAsMention())
                    .build();
            Objects.requireNonNull(event.getGuild()
                            .getChannelById(TextChannel.class, metaRepository.getSystemChannelId(event.getGuild().getIdLong())))
                    .sendMessage(message).queue(m ->
                            event.reply("Channel ready " + m.getJumpUrl()).setEphemeral(true).queue());
        }
    }

    @NotNull
    private static EmbedBuilder buildModalEmbedResponse(ModalInteractionEvent event, TextChannel newChannel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(event.getUser().getId(), null, event.getUser().getAvatarUrl());
        embedBuilder.setColor(GREEN);
        embedBuilder.setTitle(newChannel.getName() + " " + newChannel.getAsMention());
        Optional.ofNullable(event.getInteraction().getValue("channelDescription"))
                .ifPresent(m -> embedBuilder.setDescription(m.getAsString()));
        Optional.ofNullable(event.getInteraction().getValue("imageUrl"))
                .ifPresent(a -> embedBuilder.setThumbnail(a.getAsString()));
        return embedBuilder;
    }

    public void channelModal(SlashCommandInteractionEvent event) {
        TextInput subject = TextInput.create("channelName", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Name of channel")
                .setRequired(true)
                .setRequiredRange(1, 100)
                .build();
        TextInput body = TextInput.create("channelDescription", "Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Short description")
                .setRequired(false)
                .setRequiredRange(0, 3000)
                .build();
        var imageUrl = TextInput.create("imageUrl", "Channel image URL", TextInputStyle.SHORT)
                .setRequired(false);

        Optional.ofNullable(event.getOption("picture"))
                .stream()
                .map(p ->
                        imageUrl.setValue(p.getAsAttachment().getUrl())).findFirst()
                .orElseGet(() ->
                        imageUrl.setPlaceholder("https://media.discordapp.net/attachments/947891139736911892/1133409307946397696/image.png"));
        Modal modal = Modal.create("creationModal", "Channel creation")
                .addActionRow(subject)
                .addActionRow(body)
                .addActionRow(imageUrl.build())
                .build();
        event.replyModal(modal).queue();
    }
}
