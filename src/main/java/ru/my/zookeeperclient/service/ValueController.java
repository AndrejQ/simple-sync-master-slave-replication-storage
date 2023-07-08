package ru.my.zookeeperclient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import static ru.my.zookeeperclient.service.SyncReplicationAdapter.API_KEY_HEADER;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ValueController {
    private final AtomicInteger value = new AtomicInteger(-1);
    private final SyncReplicationAdapter syncReplicationAdapter;
    private final LeadershipAdapter leadershipAdapter;
    @Value("${apiKey}")
    private final String apiKey;

    @GetMapping("/get")
    public int getValue() {
        return value.get();
    }

    @PutMapping("/set/{value}")
    public ResponseEntity<Object> setValue(@PathVariable("value") int newVal) {
        if (!leadershipAdapter.isLeader()) {
            String errMsg = "Current replica is not a leader";
            log.error(errMsg);
            return ResponseEntity.badRequest().body(errMsg);
        }
        value.set(newVal);
        syncReplicationAdapter.replicate(newVal);
        return ResponseEntity.ok(value.get());
    }

    @PutMapping("/replicate")
    public ResponseEntity<Object> acceptChanges(@RequestParam("value") int newVal,
                                                @RequestHeader(API_KEY_HEADER) String apiKeyFromRequest) {
        if (apiKeyFromRequest == null || !apiKeyFromRequest.equals(apiKey)) {
            log.error("Invalid credentials");
            return ResponseEntity.status(403).build();
        }
        value.set(newVal);
        log.info("Accepted replication");
        return ResponseEntity.ok().build();
    }

    int getValueInternal() {
        return value.get();
    }
}
