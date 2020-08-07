package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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
    private int rank;
    @ManyToOne
    @JoinColumn(name="rs_event_id")
    private RsEventDto rsEventDto;


}
