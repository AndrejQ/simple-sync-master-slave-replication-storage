package ru.my.zookeeperclient.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LeadershipAdapter extends LeaderSelectorListenerAdapter {
    private static final String LEADER_PATH = "/leader";
    private final LeaderSelector leaderSelector;
    @Getter
    private volatile boolean isLeader = false;

    public LeadershipAdapter(AsyncCuratorFramework curator) {
        this.leaderSelector = new LeaderSelector(curator.unwrap(), LEADER_PATH, this);
    }

    @PostConstruct
    void start() {
        // try to elect self if leadership is lost
        leaderSelector.autoRequeue();
        // start election in background
        leaderSelector.start();
    }

    @Override
    public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
        log.info("Acquired leadership");
        final int leadershipSeconds = 10;
        try {
            isLeader = true;
            Thread.sleep(TimeUnit.SECONDS.toMillis(leadershipSeconds));
        } catch (InterruptedException e) {
            log.error("Leader interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            isLeader = false;
            log.warn("Relinquishing leadership");
        }
    }
}
