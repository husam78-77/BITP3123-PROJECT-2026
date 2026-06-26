// First Commit - Abdalla Hashim Ahmed Abdalla - B032320119
// git commit -m "Add RestTemplate config bean - Abdalla B032320119"
package com.smartcampus.courseenrolment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}