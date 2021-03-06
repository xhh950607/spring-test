package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.exception.BuyFailedException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
    RsService rsService;

    @Mock
    RsEventRepository rsEventRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    VoteRepository voteRepository;
    @Mock
    TradeRepository tradeRepository;

    LocalDateTime localDateTime;
    Vote vote;

    @BeforeEach
    void setUp() {
        initMocks(this);
        rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
        localDateTime = LocalDateTime.now();
        vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
    }

    @Test
    void shouldVoteSuccess() {
        // given

        UserDto userDto =
                UserDto.builder()
                        .voteNum(5)
                        .phone("18888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("xiaoli")
                        .id(2)
                        .build();
        RsEventDto rsEventDto =
                RsEventDto.builder()
                        .eventName("event name")
                        .id(1)
                        .keyword("keyword")
                        .voteNum(2)
                        .user(userDto)
                        .build();

        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
        // when
        rsService.vote(vote, 1);
        // then
        verify(voteRepository)
                .save(
                        VoteDto.builder()
                                .num(2)
                                .localDateTime(localDateTime)
                                .user(userDto)
                                .rsEvent(rsEventDto)
                                .build());
        verify(userRepository).save(userDto);
        verify(rsEventRepository).save(rsEventDto);
    }

    @Test
    void shouldThrowExceptionWhenUserNotExist() {
        // given
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when&then
        assertThrows(
                RuntimeException.class,
                () -> {
                    rsService.vote(vote, 1);
                });
    }

    @Test
    void shouldBuyRankSuccessWhenNoBid() {
        //given
        int amount = 100;
        int rank = 1;
        int rsEventId = 10;
        RsEventDto rsEventDto = RsEventDto.builder()
                .id(rsEventId)
                .build();
        when(rsEventRepository.findById(rsEventId)).thenReturn(Optional.of(rsEventDto));
        when(tradeRepository.findByRank(rank)).thenReturn(Optional.empty());
        Trade trade = new Trade(amount, rank);

        //when
        rsService.buy(trade, rsEventId);

        //then
        verify(tradeRepository)
                .save(TradeDto.builder()
                        .amount(amount)
                        .rank(rank)
                        .rsEventDto(rsEventDto)
                        .build());
    }

    @Test
    void shouldThrowBuyFailedExceptionWhenAmountLess() {
        //given
        int rank = 1;
        TradeDto tradeDto = TradeDto.builder()
                .amount(100)
                .rank(rank)
                .rsEventDto(new RsEventDto())
                .build();
        when(tradeRepository.findByRank(rank)).thenReturn(Optional.of(tradeDto));
        when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(new RsEventDto()));
        Trade trade = new Trade(10, rank);
        //when & then
        assertThrows(BuyFailedException.class, () -> rsService.buy(trade, 1));
    }

    @Test
    void shouldReplaceCompetitorWhenAmountMore() {
        //given
        int rank = 1;
        RsEventDto oldRsEvent = RsEventDto.builder().id(1).build();
        TradeDto tradeDto = TradeDto.builder()
                .amount(10)
                .rank(rank)
                .rsEventDto(oldRsEvent)
                .build();
        RsEventDto newRsEvent = RsEventDto.builder().id(2).build();
        when(tradeRepository.findByRank(rank)).thenReturn(Optional.of(tradeDto));
        when(rsEventRepository.findById(newRsEvent.getId())).thenReturn(Optional.of(newRsEvent));
        Trade trade = new Trade(100, rank);
        //when
        rsService.buy(trade, newRsEvent.getId());
        //then
        verify(rsEventRepository).delete(oldRsEvent);
        verify(tradeRepository)
                .save(TradeDto.builder()
                        .amount(100)
                        .rank(rank)
                        .rsEventDto(newRsEvent)
                        .build());
    }
}
