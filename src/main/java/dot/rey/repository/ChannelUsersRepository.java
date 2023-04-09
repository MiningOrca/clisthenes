package dot.rey.repository;

import dot.rey.table.ChannelUsersTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChannelUsersRepository extends JpaRepository<ChannelUsersTable, Long> {
    @Transactional
    @Modifying
    void deleteByUserIdAndChannelId(long userId, long channelId);

    Boolean existsByChannelId(long channelId);

    Optional<ChannelUsersTable> findByUserIdAndChannelId(Long userId, Long channelId);

    Boolean existsByUserIdAndChannelId(Long userId, Long channelId);


    List<ChannelUsersTable> findAllByGuildMetaTable_GuildIdAndUserId(Long guildMetaTable_guildId, Long userId);

    List<ChannelUsersTable> findAllByGuildMetaTable_GuildIdAndPrivilege(Long guildId, Integer privilege);

    @Transactional
    @Modifying
    @Query("update ChannelUsersTable c set c.privilege = :newPrivilege WHERE c.userId = :userId and c.channelId=:channelId")
    void updatePrivilege(long channelId, long userId, int newPrivilege);
}