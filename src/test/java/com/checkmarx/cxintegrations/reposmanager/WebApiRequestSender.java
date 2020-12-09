package com.checkmarx.cxintegrations.reposmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebApiRequestSender {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<JsonNode> get(String pathTemplate, int port, Object... args) throws IOException {
        return genericSend(pathTemplate, port, HttpMethod.GET, null, args);
    }

    public ResponseEntity<JsonNode> genericSend(String pathTemplate,
                                                int port,
                                                HttpMethod httpMethod,
                                                @Nullable HttpEntity<?> entity,
                                                Object... args) throws IOException {
        ResponseEntity<JsonNode> response;
        try {
            RestTemplate client = new RestTemplate();
            String fullTemplate = String.format("http://localhost:%d/%s", port, pathTemplate);
            log.info("Sending {}: {}, {}", httpMethod, fullTemplate, args);
            response = client.exchange(fullTemplate, httpMethod, entity, JsonNode.class, args);
        } catch (HttpClientErrorException e) {
            log.info("Caught a {} with the message: {}. Creating a corresponding response entity.",
                    e.getClass().getName(), e.getMessage());
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsByteArray());
            response = new ResponseEntity<>(body, e.getStatusCode());
        }
        return response;
    }
}

