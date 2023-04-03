package dot.ray.repository;

import dot.ray.table.GuildMeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessagesRepository extends JpaRepository<GuildMeta, String> {
}