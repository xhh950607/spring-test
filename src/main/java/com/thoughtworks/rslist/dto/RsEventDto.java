package com.thoughtworks.rslist.dto;

import com.thoughtworks.rslist.domain.Trade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rsEvent")
public class RsEventDto {
  @Id @GeneratedValue private int id;
  private String eventName;
  private String keyword;
  private int voteNum;
  @ManyToOne private UserDto user;

  @OneToOne(cascade = CascadeType.REMOVE, mappedBy = "rsEventDto")
  private TradeDto tradeDto;
}
