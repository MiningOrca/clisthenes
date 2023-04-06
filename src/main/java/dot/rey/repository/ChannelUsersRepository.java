package dot.rey.repository;

import dot.rey.table.ChannelUsersTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChannelUsersRepository extends JpaRepository<ChannelUsersTable, Long> {
    @Transactional
    @Modifying
    void deleteByUserIdAndChannelId(long userId, long channelId);

    Boolean existsByChannelId(long channelId);

    List<ChannelUsersTable> findAllByGuildMetaTable_GuildIdAndUserId(Long guildMetaTable_guildId, Long userId);

    @Transactional
    @Modifying
    @Query("update ChannelUsersTable c set c.isModerator = :isModerator WHERE c.userId = :userId and c.channelId=:channelId")
    void updateModerator(long channelId, long userId, boolean isModerator);
}