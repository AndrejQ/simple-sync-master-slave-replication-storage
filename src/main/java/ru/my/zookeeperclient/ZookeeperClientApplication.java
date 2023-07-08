package ru.my.zookeeperclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;


@Configuration
@SpringBootApplication
public class ZookeeperClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZookeeperClientApplication.class, args);
    }
}
