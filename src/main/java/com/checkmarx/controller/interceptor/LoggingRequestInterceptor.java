package com.checkmarx.controller.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Slf4j
@Configuration
public class LoggingRequestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object object) throws Exception {

        log.info("=============== Request {} {} ===============",request.getMethod(), request.getRequestURI());

        printHeaders(request);
        printParameters(request);
        log.info("===================================================");
        long startTime = System.currentTimeMillis();
        request.setAttribute("executionTime", startTime);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response, Object object, ModelAndView model)
            throws Exception {
        log.info("=================== Response ===================");
        log.info("[postHandle][" + request + "]");
        calcAndPrintExecution(request);
        log.info("===================================================");
    }

    private void calcAndPrintExecution(HttpServletRequest request) {
        long startTime = (Long) request.getAttribute("executionTime");
        log.info("Execution time: {} ms",
                System.currentTimeMillis() - startTime);
    }

    private void printHeaders(HttpServletRequest request) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        log.info("Request headers:");
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            final String header = request.getHeader(headerName);
            log.info("{} = {}", headerName, header);
        }
    }

    private void printParameters(HttpServletRequest request) {
        final Enumeration<String> parameterNames = request.getParameterNames();
        if (parameterNames.hasMoreElements()) {
            while (parameterNames.hasMoreElements()) {
                final String paramName = parameterNames.nextElement();
                final String paramValue = request.getParameter(paramName);
                log.info("{} = {}", paramName, paramValue);
            }
        }
    }

}


