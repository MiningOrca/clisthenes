package dot.rey.repository;

import dot.rey.table.GuildMetaTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface GuildMetaRepository extends JpaRepository<GuildMetaTable, Long> {
    boolean existsBySystemChannelId(Long aLong);

    @Query("SELECT s.systemChannelId from GuildMetaTable s where s.guildId =?1")
    Long getSystemChannelId(Long guildId);

    @Transactional
    @Modifying
    @Query("update GuildMetaTable s set s.tr21ChannelId = :channelId WHERE s.guildId = :guildId")
    void updateBanChannel(Long guildId, Long channelId);

    @Query("SELECT s.tr21ChannelId from GuildMetaTable s where s.guildId =?1")
    Long getBanChannelId(Long guildId);

    GuildMetaTable findByGuildId(Long guildId);
}