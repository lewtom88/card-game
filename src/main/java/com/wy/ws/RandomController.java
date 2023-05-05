package com.wy.ws;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
public class RandomController {

    Logger logger = LoggerFactory.getLogger(RandomController.class);

    @Value("${max_connections}")
    private int maxSessions = 50;

    @Value("${cache_duration_minute}")
    private int cacheDuration = 30;

    @Value("${pin_md5_digest}")
    private String md5Digest;

    @Value("${basic_game_numbers}")
    private String gameNumbers;

    @Value("${big_game_numbers}")
    private String bigGameNumbers;

    @Value("${extreme_games_numbers}")
    private String extremeGameNumbers;

    @Value("${ultimate_games_numbers}")
    private String ultimateGameNumbers;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    static final Cards NULL = new Cards(null);

    LoadingCache<String, Cards> cache = CacheBuilder.newBuilder()
            .maximumSize(maxSessions * 2).expireAfterAccess(cacheDuration, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, Cards>() {
                        public Cards load(String key) {
                            return NULL;
                        }
                    });


    @RequestMapping("/card_game")
    public Result<String> redirect(@RequestParam(required = false) String password,
                                   @RequestParam(required = false) String mode) {
        boolean vip = (password != null && MD5Utils.verify(password, md5Digest));

        Result<String> r = new Result<>();
        //cache需要手动清理
        if (cache.size() > maxSessions) {
            cache.cleanUp();
        }

        if (!vip && cache.size() > maxSessions) {
            r.setErrorMsg("Access denied due to session limit.");
            return r;
        }


        String id = generateID();
        Cards cards;
        if ("big".equals(mode)) {
            cards = new Cards("big", bigGameNumbers);
        } else if ("extreme".equals(mode)) {
            cards = new Cards("extreme", extremeGameNumbers);
        } else if ("ultimate".equals(mode)) {
            cards = new Cards("ultimate", ultimateGameNumbers);
        } else {
            cards = new Cards(gameNumbers);
        }
        cache.put(id, cards);
        r.setData(id);

        return r;
    }

    private String generateID() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(UUID.randomUUID().toString());
        }
        return builder.toString();
    }

    @RequestMapping("/get_cards")
    public Result<Cards> getCards(@RequestParam String id) {
        Result<Cards> r = new Result();
        try {
            Cards cards = cache.get(id);
            if (cards == NULL) {
                r.setErrorMsg("Invalid request. id: " + id);
                return r;
            }

            r.setData(cards);
        } catch (ExecutionException e) {
            logger.error("Failed to get from the cache.", e);
            r.setErrorMsg(e.getMessage());
        }

        return r;
    }

    @MessageMapping("/cards")
    public void pickCard(CardMessage cardMessage) {
        Result<Cards> r = new Result();

        String id = cardMessage.getId();
        String cardKey = cardMessage.getCardKey();
        if (id == null) {
            throw new IllegalArgumentException();
        }

        try {
            Cards cards = cache.get(id);
            if (cards == NULL) {
                r.setErrorMsg("Invalid request. id: " + id);
            } else if (cardKey == null) {
                r.setData(cards);
            } else {
                cards.pick(cardKey);
                r.setData(cards);
            }
        } catch (ExecutionException e) {
            logger.error("Failed to get from the cache.", e);
            r.setErrorMsg(e.getMessage());
        }

        simpMessagingTemplate.convertAndSend("/topic/" + id, r);
    }
}
