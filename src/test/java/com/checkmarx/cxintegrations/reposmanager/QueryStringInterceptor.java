package com.checkmarx.cxintegrations.reposmanager;

import org.mockito.invocation.InvocationOnMock;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Helps to extract query string parameters from a URL in a mock invocation.
 */
@Service
public class QueryStringInterceptor {
    public String getParamValue(InvocationOnMock invocation, String paramName) {
        MultiValueMap<String, String> urlQuery = extractUrlQuery(invocation);
        return getQueryParamValue(urlQuery, paramName);
    }

    private static MultiValueMap<String, String> extractUrlQuery(InvocationOnMock invocation) {
        String url = invocation.getArgument(0);

        return UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams();
    }

    private static String getQueryParamValue(MultiValueMap<String, String> urlQuery, String paramName) {
        String result = urlQuery.getFirst(paramName);
        if (result == null) {
            throw new RuntimeException(String.format("The '%s' parameter is missing in query string.", paramName));
        }
        return result;
    }
}
