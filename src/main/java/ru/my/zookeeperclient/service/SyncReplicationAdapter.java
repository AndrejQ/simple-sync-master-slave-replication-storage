package ru.my.zookeeperclient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncReplicationAdapter {
    private final RestTemplate restTemplate = new RestTemplate();
    private final DiscoveryAdapter discoveryAdapter;

    public static final String API_KEY_HEADER = "x-api-key";
    @Value("${apiKey}")
    private final String apiKey;

    @Value("${server.port}")
    private final int port;

    void replicate(int value) {
        /*
         Тут надо обрабатывать случай частичного обновления слейв-реплик:
         ретраить до победного или исключать их из списка активных синхронных реплик
        */
        discoveryAdapter.getMembersExceptSelf().stream().parallel()
                .forEach(inst -> {
                    String url = "http://%s:%d/replicate".formatted(inst, port);

                    MultiValueMap<String, Integer> params = new LinkedMultiValueMap<>();
                    params.add("value", value);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(API_KEY_HEADER, apiKey);

                    ResponseEntity<Void> responseEntity = restTemplate
                            .exchange(url, HttpMethod.PUT, new HttpEntity<>(params, headers), Void.class);

                    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                        throw new RuntimeException("Replication failed, synchronisation lost, plz restart containers");
                    }
                });

    }
}
