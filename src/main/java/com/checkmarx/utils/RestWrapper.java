package com.checkmarx.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class RestWrapper {

    public static final String GENERAL_RUNTIME_EXCEPTION = "Repos-Manager Generic exception, " +
            "Check logs";
    public static final String SAVE_ACCESS_TOKEN_FAILURE = "Save access token failure - Wasn't " +
            "able to save in database via DataStore service";
    public static final String SAVE_SCM_ORG_FAILURE = "Save organization failure - Wasn't " +
            "able to save in database via DataStore service";
    public static final String GENERATE_ACCESS_TOKEN_FAILURE = "Access token generation failure";
    public static final String ACCESS_TOKEN_MISSING = "Access token is missing from local map, " +
            "please reconnect";
    public static final String SCM_DETAILS_MISSING = "SCM details are missing, Missing Client id " +
            "& " +
            "Client Secret";
    public static final String SCM_ORG_TOKEN_MISSING = "SCM organization token is missing";
    public static final String STORE_SCM_FAILURE = "Store scm failure - Wasn't able to save in " +
            "database via DataStore service";
    public static final String STORE_SCM_ORG_REPOS_FAILURE = "Store scm org repositories failure " +
            "- Wasn't able to save in database via DataStore service";
    private static final String CANNOT_GET_FROM_DATASTORE = "- Wasn't able to get from database via DataStore service";
    public static final String GET_ORG_FAILURE = "Get org failure " +
            CANNOT_GET_FROM_DATASTORE;
    public static final String GET_ORG_REPOS_FAILURE = "Get org repositories failure " +
            CANNOT_GET_FROM_DATASTORE;
    public static final String MISSING_ORG_REPO = "Org repository failure " +
            "- Missing org repository in database";
    public static final String GET_ORG_REPO_FAILURE = "Get org repository failure " +
            CANNOT_GET_FROM_DATASTORE;
    public static final String WEBHOOK_CREATE_FAILURE = "Failed to create repo webhook";
    public static final String WEBHOOK_DELETE_FAILURE = "Failed to delete repo webhook";
    public static final String SCM_NOT_SUPPORTED = "Given Scm isn't supported";
    public static final String UPDATE_ORG_REPO_FAILURE = "Update org repository failure " +
            "- Wasn't able to update database via DataStore service";
    public static final String MISSING_SCM_ORG = "Scm org failure - missing scm organization in " +
            "database";
    public static final String STORE_SCM_ORG_PROPERTIES_FAILURE = "Store scm org settings " +
            "failure " +
            "- Wasn't able to save in database via DataStore service";
    public static final String GET_SCM_ORG_PROPERTIES_FAILURE = "Get org settings failure " +
            CANNOT_GET_FROM_DATASTORE;

    @Autowired
    RestTemplate restTemplate;

   /**
     * sendRequest method used as rest request template, sends request via RestTemplate
     *
     * @param path         url path
     * @param method       http method
     * @param body      request body
     * @param headerMap      request headers as map
     * @param responseType expected class structure as response
     * @return ResponseEntity of any type
     */
    public ResponseEntity sendRequest(String path, HttpMethod method,
                                      Object body,
                                      Map<String, String> headerMap,
                                      Class responseType) {
        HttpHeaders headers = createHeaders(headerMap);
        final HttpEntity<String> request = createRequest(body, headers);
        return restTemplate.exchange(path, method, request, responseType);
    }

    /**
     * sendRequest method used as rest request template, sends request via RestTemplate
     *
     * @param path         url path
     * @param mapPostBody      request body in a form or a map
     * @param headerMap      request headers 
     * @param responseType expected class structure as response
     * @return ResponseEntity of any type
     */
    public <T> ResponseEntity<T> sendUrlEncodedPostRequest(String path,
                                                    MultiValueMap<String, String> mapPostBody,
                                                    Map<String, String> headerMap,
                                                    Class<T> responseType) {

        HttpHeaders headers = createHeaders(headerMap);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(mapPostBody, headers);
        return restTemplate.postForEntity(path, request, responseType);
    }

    /**
     * sendRequest method used as rest request template, sends request via RestTemplate
     *
     * @param path         url path
     * @param mapPostBody      request body in a form or a map
     * @param headers      request headers as map
     * @param responseType expected class structure as response
     * @return ResponseEntity of any type
     */
    public <T> ResponseEntity<T> sendUrlEncodedPostRequest(String path,
                                                    MultiValueMap<String, String> mapPostBody,
                                                    HttpHeaders headers,
                                                    Class<T> responseType) {

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(mapPostBody, headers);
        return restTemplate.postForEntity(path, request, responseType);
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

    /**
     * sendBearerAuthRequest method used as rest request template with bearer token in header,
     * sends request via RestTemplate
     *
     * @param path      url path
     * @param method    http method
     * @param body      request body
     * @param headerMap      request headers as map
     * @param responseType expected class structure as response
     * @param token access token
     * @return ResponseEntity of any type
     */
    public <T> ResponseEntity<T> sendBearerAuthRequest(String path, HttpMethod method, Object body,
                                      Map<String, String> headerMap , Class<T> responseType,
                                      String token) {
        HttpHeaders headers = createHeaders(headerMap);
        headers.setBearerAuth(token);
        final HttpEntity<String> request = createRequest(body, headers);
        return restTemplate.exchange(path, method, request, responseType);

    }
}
