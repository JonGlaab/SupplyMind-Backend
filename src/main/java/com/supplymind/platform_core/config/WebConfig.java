package com.supplymind.platform_core.config;



import com.supplymind.platform_core.common.util.PaginationDefaults;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    @org.springframework.context.annotation.Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(PaginationDefaults.MAX_PAGE_SIZE);
            resolver.setOneIndexedParameters(false); // page=0 is first page
        };
    }
}

