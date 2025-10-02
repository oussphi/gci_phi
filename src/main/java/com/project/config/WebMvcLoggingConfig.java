package com.project.config;

import com.project.repository.RequestLogRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcLoggingConfig implements WebMvcConfigurer {

    private final RequestLogRepository requestLogRepository;

    public WebMvcLoggingConfig(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new com.project.web.RequestLoggingInterceptor(requestLogRepository))
                .addPathPatterns("/**");
    }
}


