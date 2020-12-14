package com.checkmarx.cxintegrations.reposmanager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebApiRequestSender {

    public ResponseEntity<String> get(String pathTemplate, int port, Object... args) {
        return genericSend(pathTemplate, port, HttpMethod.GET, null, args);
    }

    public ResponseEntity<String> genericSend(String pathTemplate,
                                                int port,
                                                HttpMethod httpMethod,
                                                @Nullable HttpEntity<?> entity,
                                                Object... args) {
        ResponseEntity<String> response;
        try {
            RestTemplate client = new RestTemplate();
            String fullTemplate = String.format("http://localhost:%d/%s", port, pathTemplate);
            log.info("Sending {}: {}, {}", httpMethod, fullTemplate, args);
            response = client.exchange(fullTemplate, httpMethod, entity, String.class, args);
        } catch (HttpClientErrorException e) {
            log.info("Caught a {} with the message: {}. Creating a corresponding response entity.",
                    e.getClass().getName(), e.getMessage());
            response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        }
        return response;
    }
}

