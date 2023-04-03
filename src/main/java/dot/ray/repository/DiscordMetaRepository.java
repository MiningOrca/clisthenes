package dot.ray.repository;

import dot.ray.table.GuildMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscordMetaRepository extends JpaRepository<GuildMeta, Long> {
    Optional<GuildMeta> getGuildMetaByGuildId(long guildId);
}