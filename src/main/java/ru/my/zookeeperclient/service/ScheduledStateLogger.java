package ru.my.zookeeperclient.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledStateLogger {
    private final DiscoveryAdapter discoveryAdapter;
    private final LeadershipAdapter leadershipAdapter;
    private final ValueController valueController;

    @PostConstruct
    void start() {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() ->
                log.info("leader: {}, value: {}, members: {}",
                        leadershipAdapter.isLeader(),
                        valueController.getValueInternal(),
                        discoveryAdapter.getMembers()), 1, 10, SECONDS);
    }
}
