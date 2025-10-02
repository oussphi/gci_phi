package com.project.web;

import com.project.entity.RequestLog;
import com.project.repository.RequestLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private final RequestLogRepository repository;

    public RequestLoggingInterceptor(RequestLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        long start = System.currentTimeMillis();
        request.setAttribute("_req_start", start);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        RequestLog log = new RequestLog();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.setUsername(auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous");
        log.setMethod(request.getMethod());
        log.setPath(request.getRequestURI());
        if (handler instanceof HandlerMethod hm) {
            log.setController(hm.getBeanType().getSimpleName());
            log.setHandler(hm.getMethod().getName());
        }
        log.setStatus(response.getStatus());
        Object start = request.getAttribute("_req_start");
        if (start instanceof Long s) {
            log.setDurationMs(System.currentTimeMillis() - s);
        }
        log.setTimestamp(Instant.now());
        repository.save(log);
    }
}


