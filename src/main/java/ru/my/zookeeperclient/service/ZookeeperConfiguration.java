package ru.my.zookeeperclient.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ZookeeperConfiguration {
    @Bean
    AsyncCuratorFramework asyncCuratorFramework(@Value("${env.zookeeper}") String zookeeperHost, RetryPolicy retryPolicy) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperHost, retryPolicy);
        client.start();
        return AsyncCuratorFramework.wrap(client);
    }

    @Bean
    RetryPolicy retryPolicy() {
        int sleepMsBetweenRetries = 100;
        int maxRetries = 3;
        return new RetryNTimes(maxRetries, sleepMsBetweenRetries);
    }
}
