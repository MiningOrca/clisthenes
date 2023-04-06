package dot.rey.repository;

import dot.rey.table.BanTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;

public interface BanRepository extends JpaRepository<BanTable, Long> {
    @Transactional
    @Modifying
    void deleteByChannelIdAndUserId(Long channelId, Long userId);

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);

    List<BanTable> findAllByGuildMetaTable_GuildId(Long guildMetaTable_guildId);

    List<BanTable> findAllByGuildMetaTable_GuildIdAndUserId(Long guildId, Long userId);
}
