package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "trade")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDto {

    @Id
    @GeneratedValue
    private int id;
    private int amount;
    @Column(unique = true)
    private int rank;
    @OneToOne
    @JoinColumn(name = "rs_event_id")
    private RsEventDto rsEventDto;
}
