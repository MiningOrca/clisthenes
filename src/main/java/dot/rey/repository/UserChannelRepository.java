package dot.rey.repository;

import dot.rey.table.ChannelsTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.List;

public interface UserChannelRepository extends JpaRepository<ChannelsTable, Long> {
    List<ChannelsTable> findAllByOwnerIdAndGuildMetaTable_GuildId(Long ownerId, Long guildId);

    ArrayList<Long> findAllByParentChannelId(Long parentChannelId);
}