package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.exception.BuyFailedException;
import com.thoughtworks.rslist.exception.Error;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.service.RsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Validated
public class RsController {
    @Autowired
    RsEventRepository rsEventRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TradeRepository tradeRepository;
    @Autowired
    RsService rsService;

    @GetMapping("/rs/list")
    public ResponseEntity<List<RsEvent>> getRsEventListBetween(
            @RequestParam(required = false) Integer start, @RequestParam(required = false) Integer end) {
        List<RsEventDto> rsEventDtos = rsEventRepository.findAll();
        List<RsEvent> rsEvents = new ArrayList<>();
        for(int i=0;i<rsEventDtos.size();i++){
            rsEvents.add(null);
        }

        List<TradeDto> tradeDtos = tradeRepository.findAll();
        List<Integer> tradeRsEventIds = tradeDtos.stream()
                .map(tradeDto -> tradeDto.getRsEventDto().getId())
                .collect(Collectors.toList());

        rsEventDtos = rsEventDtos.stream()
                .filter(rsEventDto -> !tradeRsEventIds.contains(rsEventDto.getId()))
                .sorted(Comparator.comparing(RsEventDto::getVoteNum).reversed())
                .collect(Collectors.toList());
        Iterator<RsEventDto> rsEventDtoIterator = rsEventDtos.iterator();

        for (TradeDto tradeDto : tradeDtos) {
            RsEventDto rsEventDto = tradeDto.getRsEventDto();
            RsEvent rsEvent = RsEvent.builder()
                    .eventName(rsEventDto.getEventName())
                    .keyword(rsEventDto.getKeyword())
                    .voteNum(rsEventDto.getVoteNum())
                    .userId(rsEventDto.getUser().getId())
                    .build();
            rsEvents.set(tradeDto.getRank() - 1, rsEvent);
        }

        for(int i=0;i<rsEvents.size();i++){
            if(rsEvents.get(i)==null){
                RsEventDto rsEventDto = rsEventDtoIterator.next();
                RsEvent rsEvent = RsEvent.builder()
                        .eventName(rsEventDto.getEventName())
                        .keyword(rsEventDto.getKeyword())
                        .voteNum(rsEventDto.getVoteNum())
                        .userId(rsEventDto.getUser().getId())
                        .build();
                rsEvents.set(i, rsEvent);
            }
        }

        if (start == null || end == null) {
            return ResponseEntity.ok(rsEvents);
        }
        return ResponseEntity.ok(rsEvents.subList(start - 1, end));
    }

    @GetMapping("/rs/{index}")
    public ResponseEntity<RsEvent> getRsEvent(@PathVariable int index) {
        List<RsEvent> rsEvents =
                rsEventRepository.findAll().stream()
                        .map(
                                item ->
                                        RsEvent.builder()
                                                .eventName(item.getEventName())
                                                .keyword(item.getKeyword())
                                                .userId(item.getId())
                                                .voteNum(item.getVoteNum())
                                                .build())
                        .collect(Collectors.toList());
        if (index < 1 || index > rsEvents.size()) {
            throw new RequestNotValidException("invalid index");
        }
        return ResponseEntity.ok(rsEvents.get(index - 1));
    }

    @PostMapping("/rs/event")
    public ResponseEntity addRsEvent(@RequestBody @Valid RsEvent rsEvent) {
        Optional<UserDto> userDto = userRepository.findById(rsEvent.getUserId());
        if (!userDto.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        RsEventDto build =
                RsEventDto.builder()
                        .keyword(rsEvent.getKeyword())
                        .eventName(rsEvent.getEventName())
                        .voteNum(0)
                        .user(userDto.get())
                        .build();
        rsEventRepository.save(build);
        return ResponseEntity.created(null).build();
    }

    @PostMapping("/rs/vote/{id}")
    public ResponseEntity vote(@PathVariable int id, @RequestBody Vote vote) {
        rsService.vote(vote, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rs/buy/{id}")
    public ResponseEntity buy(@PathVariable int id, @RequestBody Trade trade) {
        try {
            rsService.buy(trade, id);
            return ResponseEntity.ok().build();
        } catch (BuyFailedException e) {
            return ResponseEntity.badRequest().build();
        }
    }


    @ExceptionHandler(RequestNotValidException.class)
    public ResponseEntity<Error> handleRequestErrorHandler(RequestNotValidException e) {
        Error error = new Error();
        error.setError(e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
