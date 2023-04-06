package dot.rey.repository;

import dot.rey.table.ChannelUsersTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChannelUsersRepository extends JpaRepository<ChannelUsersTable, Long> {

    void deleteByUserIdAndChannelId(long userId, long channelId);

    Boolean existsByChannelId(long channelId);

    List<ChannelUsersTable> findAllByGuildMetaTable_GuildIdAndUserId(Long guildMetaTable_guildId, Long userId);

    @Query("select s.isBanned from ChannelUsersTable s where s.channelId =?1 and s.userId =?2")
    Optional<Boolean> isBanned(long channelId, long userId);

    @Transactional
    @Modifying
    @Query("update ChannelUsersTable c set c.isBanned = :isBanned WHERE c.userId = :userId and c.channelId=:channelId")
    void updateBanned(long channelId, long userId, boolean isBanned);

    @Transactional
    @Modifying
    @Query("update ChannelUsersTable c set c.isModerator = :isModerator WHERE c.userId = :userId and c.channelId=:channelId")
    void updateModerator(long channelId, long userId, boolean isModerator);
}