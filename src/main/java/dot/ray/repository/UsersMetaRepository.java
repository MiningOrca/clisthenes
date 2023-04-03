package dot.ray.repository;

import dot.ray.table.UsersMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersMetaRepository extends JpaRepository<UsersMeta, String> {
    Optional<UsersMeta> findByGuildId(long guildId);
}