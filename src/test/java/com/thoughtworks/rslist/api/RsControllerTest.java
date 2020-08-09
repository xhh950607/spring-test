package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RsEventRepository rsEventRepository;
    @Autowired
    VoteRepository voteRepository;
    @Autowired
    TradeRepository tradeRepository;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        voteRepository.deleteAll();
        rsEventRepository.deleteAll();
        userRepository.deleteAll();
        userDto =
                UserDto.builder()
                        .voteNum(10)
                        .phone("188888888888")
                        .gender("female")
                        .email("a@b.com")
                        .age(19)
                        .userName("idolice")
                        .build();
    }

    @Test
    public void shouldGetRsEventList() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);

        mockMvc
                .perform(get("/rs/list"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventName", is("第一条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[0]", not(hasKey("user"))))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetOneEvent() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.eventName", is("第一条事件")));
        mockMvc.perform(get("/rs/1")).andExpect(jsonPath("$.keyword", is("无分类")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.eventName", is("第二条事件")));
        mockMvc.perform(get("/rs/2")).andExpect(jsonPath("$.keyword", is("无分类")));
    }

    @Test
    public void shouldGetErrorWhenIndexInvalid() throws Exception {
        mockMvc
                .perform(get("/rs/4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid index")));
    }

    @Test
    public void shouldGetRsListBetween() throws Exception {
        UserDto save = userRepository.save(userDto);

        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();

        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第二条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        rsEventDto = RsEventDto.builder().keyword("无分类").eventName("第三条事件").user(save).build();
        rsEventRepository.save(rsEventDto);
        mockMvc
                .perform(get("/rs/list?start=1&end=2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第一条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=2&end=3"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")));
        mockMvc
                .perform(get("/rs/list?start=1&end=3"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].keyword", is("无分类")))
                .andExpect(jsonPath("$[1].eventName", is("第二条事件")))
                .andExpect(jsonPath("$[1].keyword", is("无分类")))
                .andExpect(jsonPath("$[2].eventName", is("第三条事件")))
                .andExpect(jsonPath("$[2].keyword", is("无分类")));
    }

    @Test
    public void shouldAddRsEventWhenUserExist() throws Exception {

        UserDto save = userRepository.save(userDto);

        String jsonValue =
                "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": " + save.getId() + "}";

        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
        List<RsEventDto> all = rsEventRepository.findAll();
        assertNotNull(all);
        assertEquals(all.size(), 1);
        assertEquals(all.get(0).getEventName(), "猪肉涨价了");
        assertEquals(all.get(0).getKeyword(), "经济");
        assertEquals(all.get(0).getUser().getUserName(), save.getUserName());
        assertEquals(all.get(0).getUser().getAge(), save.getAge());
    }

    @Test
    public void shouldAddRsEventWhenUserNotExist() throws Exception {
        String jsonValue = "{\"eventName\":\"猪肉涨价了\",\"keyword\":\"经济\",\"userId\": 100}";
        mockMvc
                .perform(post("/rs/event").content(jsonValue).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVoteSuccess() throws Exception {
        UserDto save = userRepository.save(userDto);
        RsEventDto rsEventDto =
                RsEventDto.builder().keyword("无分类").eventName("第一条事件").user(save).build();
        rsEventDto = rsEventRepository.save(rsEventDto);

        String jsonValue =
                String.format(
                        "{\"userId\":%d,\"time\":\"%s\",\"voteNum\":1}",
                        save.getId(), LocalDateTime.now().toString());
        mockMvc
                .perform(
                        post("/rs/vote/{id}", rsEventDto.getId())
                                .content(jsonValue)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        UserDto userDto = userRepository.findById(save.getId()).get();
        RsEventDto newRsEvent = rsEventRepository.findById(rsEventDto.getId()).get();
        assertEquals(userDto.getVoteNum(), 9);
        assertEquals(newRsEvent.getVoteNum(), 1);
        List<VoteDto> voteDtos = voteRepository.findAll();
        assertEquals(voteDtos.size(), 1);
        assertEquals(voteDtos.get(0).getNum(), 1);
    }

    @Test
    public void shouldBuySuccessWhenNoBidding() throws Exception {
        userDto = userRepository.save(userDto);
        RsEventDto rsEventDto = RsEventDto.builder()
                .eventName("event name")
                .keyword("keyword")
                .voteNum(0)
                .user(userDto)
                .build();
        rsEventDto = rsEventRepository.save(rsEventDto);
        String postBody = "{\"amount\":100,\"rank\":1}";
        mockMvc.perform(post("/rs/buy/" + rsEventDto.getId())
                .content(postBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        List<TradeDto> tradeDtos = tradeRepository.findAll();
        assertEquals(1, tradeDtos.size());
        TradeDto tradeDto = tradeDtos.get(0);
        assertEquals(100, tradeDto.getAmount());
        assertEquals(1, tradeDto.getRank());
        assertEquals(rsEventDto.getId(), tradeDto.getRsEventDto().getId());
    }

    @Test
    public void shouldBadRequestWhenAmountLess() throws Exception {
        userDto = userRepository.save(userDto);
        RsEventDto rsEventDto = RsEventDto.builder()
                .eventName("event old")
                .keyword("keyword")
                .voteNum(0)
                .user(userDto)
                .build();
        rsEventDto = rsEventRepository.save(rsEventDto);
        TradeDto tradeDto = TradeDto.builder()
                .amount(200)
                .rank(1)
                .rsEventDto(rsEventDto)
                .build();
        tradeRepository.save(tradeDto);

        String postBody = "{\"amount\":100,\"rank\":1}";
        mockMvc.perform(post("/rs/buy/" + rsEventDto.getId())
                .content(postBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReplaceWhenAmountMore() throws Exception {
        userDto = userRepository.save(userDto);
        RsEventDto rsEventDto = RsEventDto.builder()
                .eventName("event old")
                .keyword("keyword")
                .voteNum(0)
                .user(userDto)
                .build();
        rsEventDto = rsEventRepository.save(rsEventDto);
        TradeDto tradeDto = TradeDto.builder()
                .amount(100)
                .rank(1)
                .rsEventDto(rsEventDto)
                .build();
        tradeRepository.save(tradeDto);

        rsEventDto = RsEventDto.builder()
                .eventName("event new")
                .keyword("keyword")
                .voteNum(0)
                .user(userDto)
                .build();
        rsEventDto = rsEventRepository.save(rsEventDto);
        String postBody = "{\"amount\":200,\"rank\":1}";
        mockMvc.perform(post("/rs/buy/" + rsEventDto.getId())
                .content(postBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        tradeDto = tradeRepository.findByRank(1).get();
        assertEquals(200, tradeDto.getAmount());
        assertEquals(rsEventDto.getId(), tradeDto.getRsEventDto().getId());
    }

    @Test
    public void shouldGetRsListAfterTrade() throws Exception {
        userRepository.save(userDto);
        rsEventRepository.save(RsEventDto.builder()
                .eventName("event 1 with vote 2")
                .voteNum(2)
                .user(userDto)
                .build());
        rsEventRepository.save(RsEventDto.builder()
                .eventName("event 2 with vote 3")
                .voteNum(3)
                .user(userDto)
                .build());
        RsEventDto rsEventDto = rsEventRepository.save(RsEventDto.builder()
                .eventName("event 3 with vote 0")
                .voteNum(0)
                .user(userDto)
                .build());

        String postBody = "{\"amount\":200,\"rank\":1}";
        mockMvc.perform(post("/rs/buy/" + rsEventDto.getId())
                .content(postBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rs/list"))
                .andExpect(jsonPath("$[0].eventName").value("event 3 with vote 0"))
                .andExpect(jsonPath("$[1].eventName").value("event 2 with vote 3"))
                .andExpect(jsonPath("$[2].eventName").value("event 1 with vote 2"))
                .andExpect(status().isOk());
    }
}
