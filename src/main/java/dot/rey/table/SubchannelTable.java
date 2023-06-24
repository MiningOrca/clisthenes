package dot.rey.table;

import javax.persistence.*;

@Entity
@Table(name = "sub_channels_table")
public class SubchannelTable {

    @ManyToOne
    @JoinColumn(name = "channel_id", nullable = false)
    private ChannelsTable channelsTable;

    @Id
    @Column(name = "sub_channel", nullable = false, unique = true)
    private Long subchannel;

}
