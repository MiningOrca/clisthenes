package dot.ray.repository;

import dot.ray.table.ChannelMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuildChannelRepository extends JpaRepository<ChannelMeta, Long> {
    void deleteByChannelIdAndGuildId(long channelId, long guildId);

    Optional<ChannelMeta> findByChannelIdAndGuildId(long channelId, long guildId);
}