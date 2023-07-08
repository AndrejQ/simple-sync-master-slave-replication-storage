package ru.my.zookeeperclient.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedMap;
import static java.util.function.Predicate.not;
import static org.apache.zookeeper.AddWatchMode.PERSISTENT_RECURSIVE;
import static org.apache.zookeeper.KeeperException.Code.NODEEXISTS;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeCreated;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;

@Slf4j
@Component
@RequiredArgsConstructor
// Не использовал curator-recipes специально для умственной тренировки и профилактики деменции
public class DiscoveryAdapter {
    private static final String MEMBERS_PATH = "/members";

    private final AsyncCuratorFramework curator;
    @Value("${env.serviceUrl}")
    private final String currentServiceAddress;
    private final Map<String, String> members = synchronizedMap(new HashMap<>());
    private final CountDownLatch membersLatch = new CountDownLatch(1);

    private final AtomicReference<String> currentServiceNodePath = new AtomicReference<>();

    @PostConstruct
    void start() throws ExecutionException, InterruptedException {
        try {
            curator.unwrap().create().forPath(MEMBERS_PATH);
        } catch (Exception e) {
            if (e instanceof KeeperException keeperException && keeperException.code() == NODEEXISTS) {
                log.info(e.getMessage());
            } else {
                throw new RuntimeException(e);
            }
        }

        CompletableFuture.allOf(
                registerCurrentInstance(),
                reactToMembersChange()
        ).get();
    }

    private CompletableFuture<CompletionStage<Void>> reactToMembersChange() {
        return curator.addWatch()
                .withMode(PERSISTENT_RECURSIVE)
                .usingWatcher((CuratorWatcher) event -> {
                    membersLatch.await();
                    log.info("Service {}, change event: {}", currentServiceAddress, event);
                    String path = event.getPath();
                    if (event.getType() == NodeCreated || event.getType() == NodeDataChanged) {
                        curator.getData().forPath(path)
                                .thenApply(String::new)
                                .thenAccept(location -> members.put(path, location));
                    }
                    if (event.getType() == NodeDeleted) {
                        members.remove(path);
                    }
                })
                .forPath(MEMBERS_PATH)
                .thenApply(__ -> fetchMembers())
                .toCompletableFuture();
    }

    private CompletableFuture<Void> registerCurrentInstance() {
        return curator.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(MEMBERS_PATH + "/inst", currentServiceAddress.getBytes())
                .thenAccept(currentServiceNodePath::set)
                .toCompletableFuture();
    }

    CompletionStage<Void> fetchMembers() {
        return curator.getChildren()
                .forPath(MEMBERS_PATH)
                .thenAccept(nodes -> nodes
                        .stream()
                        .map(node -> MEMBERS_PATH + "/" + node)
                        .parallel()
                        .forEach(path -> curator.getData()
                                .forPath(path)
                                .toCompletableFuture()
                                .thenAcceptAsync(memberBytes ->
                                        members.put(path, new String(memberBytes)))))
                .thenAccept(__ -> membersLatch.countDown());
    }

    Set<String> getMembersExceptSelf() {
        return members.values().stream()
                .filter(not(currentServiceAddress::equals))
                .collect(Collectors.toSet());
    }

    Set<String> getMembers() {
        return Set.copyOf(members.values());
    }
}
