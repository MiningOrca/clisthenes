package dot.rey.repository;

import dot.rey.table.ChannelsTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface UserChannelRepository extends JpaRepository<ChannelsTable, Long> {
    Optional<List<ChannelsTable>> findAllByOwnerId(Long ownerId);

    ArrayList<Long> findAllByParentChannelId(Long parentChannelId);
}