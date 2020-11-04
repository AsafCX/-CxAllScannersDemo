package com.checkmarx.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class RestHelper {

    public static final String SAVE_ACCESS_TOKEN_FAILURE = "Save access token failure - Wasn't " +
            "able to save in database via DataStore service";
    public static final String GENERATE_ACCESS_TOKEN_FAILURE = "Access token generation failure";
    public static final String ACCESS_TOKEN_MISSING = "Access token is missing, please reconnect";

    @Autowired
    RestTemplate restTemplate;

    /**
     * sendRequest method used as rest request template, sends request via RestTemplate
     *
     * @param path         url path
     * @param method       http method
     * @param request      request including headers and (optional) body
     * @param responseType expected class structure as response
     * @return ResponseEntity of any type
     */
    public ResponseEntity sendRequest(String path, HttpMethod method,
                                      HttpEntity<String> request,
                                      Class responseType) {
        return restTemplate.exchange(path, method, request, responseType);
    }

    /**
     * createRequest method used as request creation template, Construct request from given
     * headers and body
     *
     * @param body    request body
     * @param headers http headers
     * @return HttpEntity including headers and body sent as input
     */
    public HttpEntity<String> createRequest(Object body, HttpHeaders headers) {
        return new HttpEntity( body, headers);
    }

    /**
     * createHeaders method created headers for future Rest request, Adding user auth token for
     * GitHub authorization
     *
     * @param headerMap set of key-value map to add as request headers
     * @return HttpHeaders for future rest request
     */
    public HttpHeaders createHeaders(Map<String, String> headerMap) {
        final HttpHeaders headers = new HttpHeaders();
        if (headerMap != null) {
            for (Map.Entry<String, String> header : headerMap.entrySet()) {
                headers.add(header.getKey(), header.getValue());
            }
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

}
