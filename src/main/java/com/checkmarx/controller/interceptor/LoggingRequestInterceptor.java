package com.checkmarx.controller.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class LoggingRequestInterceptor implements HandlerInterceptor {

    StringBuilder stringBuilder;
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object object) throws Exception {
        stringBuilder = new StringBuilder();
        stringBuilder.append("\n=============== Request ").append(request.getMethod()).append(" ")
                .append(request.getRequestURI()).append(" ===============\n");
        printHeaders(request);
        printParameters(request);
        if (! HttpMethod.GET.matches(request.getMethod())){
            printBody(request);
        }
        stringBuilder.append("===================================================\n");
        log.trace(stringBuilder.toString());
        long startTime = System.currentTimeMillis();
        request.setAttribute("executionTime", startTime);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object object, ModelAndView model) {
        log.trace("=================== Response ===================");
        log.trace("[postHandle][" + response + "]");
        calcAndPrintExecution(request);
        log.trace("===================================================");
    }

    private void calcAndPrintExecution(HttpServletRequest request) {
        long startTime = (Long) request.getAttribute("executionTime");
        log.trace("Execution time: {} ms",
                System.currentTimeMillis() - startTime);
    }

    private void printHeaders(HttpServletRequest request) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        stringBuilder.append("      Request headers\n");
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final String header = request.getHeader(headerName);
            stringBuilder.append(headerName).append(" = ").append(header).append("\n");
        }
    }

    private void printParameters(HttpServletRequest request) {
        final Enumeration<String> parameterNames = request.getParameterNames();
        if (parameterNames.hasMoreElements()) {
            while (parameterNames.hasMoreElements()) {
                final String paramName = parameterNames.nextElement();
                final String paramValue = request.getParameter(paramName);
                stringBuilder.append(paramName).append(" = ").append(paramValue).append("\n");
            }
        }
    }

    private void printBody(HttpServletRequest request) throws IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            stringBuilder.append("      Request Body\n");
            stringBuilder.append(request.getReader().lines()
                                         .collect(Collectors.joining(System.lineSeparator())))
                    .append("\n");
        }
    }

}


