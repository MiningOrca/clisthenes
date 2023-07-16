package dot.rey.discord;

import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

import static net.dv8tion.jda.api.Permission.*;

public class Utils {

    public static final String YES_REACTION = "☑";
    public static final String NO_REACTION = "✖";
    public static String systemChannelName = "breeding-channel";

    public static String centralBanChannelName = "system-channel-tr21";
    public static EnumSet<Permission> textChannelAdminPermission = initTextAdminPermission();

    public static EnumSet<Permission> textChannelModeratorPermission = initTextModeratorPermission();
    public static String helpText = """
            For now 3 commands:
             ban<@user_id><#channel_id> - to 'ban' user in channel
             forgive<@user_id><#channel_id> - to 'unban' user in channel
             getbans - to get list of all banned users
             spyuser<@user_id> - to get list of all user channels
            \s""";
    public static EnumSet<Permission> voiceChannelUserPermission = initVoiceUserPermission();

    private static EnumSet<Permission> initVoiceUserPermission() {
        var perm = Permission.getRaw(VOICE_STREAM, VOICE_CONNECT, VOICE_SPEAK, VOICE_USE_VAD,
                REQUEST_TO_SPEAK, VOICE_START_ACTIVITIES, VIEW_CHANNEL);
        return Permission.getPermissions(perm);
    }

    private static EnumSet<Permission> initTextModeratorPermission() {
        var perm = Permission.getPermissions(ALL_TEXT_PERMISSIONS);
        perm.add(VIEW_CHANNEL);
        perm.add(MESSAGE_MANAGE);
        return perm;
    }

    public static EnumSet<Permission> textChannelUserPermission = initTextUserPermission();

    private static EnumSet<Permission> initTextUserPermission() {
        var perm = Permission.getRaw(MESSAGE_ADD_REACTION, MESSAGE_SEND,
                MESSAGE_EMBED_LINKS, MESSAGE_ATTACH_FILES, MESSAGE_EXT_EMOJI, MESSAGE_EXT_STICKER,
                MESSAGE_HISTORY, USE_APPLICATION_COMMANDS,
                CREATE_PUBLIC_THREADS, MESSAGE_SEND_IN_THREADS, VIEW_CHANNEL);
        return Permission.getPermissions(perm);
    }


    @NotNull
    private static EnumSet<Permission> initTextAdminPermission() {
        var perm = Permission.getPermissions(ALL_TEXT_PERMISSIONS);
        perm.add(MESSAGE_MANAGE);
        perm.add(MANAGE_CHANNEL);
        perm.add(VIEW_CHANNEL);
        return perm;
    }

    public enum Privilege {
        UNBANNED(0), BAN(-1), USER(1), MODERATOR(8), CHOSEN_ADMIN(9), OWNER(10);
        final int offset;

        public int getOffset() {
            return offset;
        }

        Privilege(int offset) {
            this.offset = offset;
        }

        public static Privilege getFromOffset(int offset) {
            for (Privilege perm : values()) {
                if (perm.offset == offset)
                    return perm;
            }
            return UNBANNED; //this should never happens
        }
    }
}
