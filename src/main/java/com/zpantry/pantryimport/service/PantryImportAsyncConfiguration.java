package com.zpantry.pantryimport.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class PantryImportAsyncConfiguration {
    @Bean("unmatchedIngredientExecutor")
    ThreadPoolTaskExecutor unmatchedIngredientExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("unmatched-ingredient-");
        executor.initialize();
        return executor;
    }
}
