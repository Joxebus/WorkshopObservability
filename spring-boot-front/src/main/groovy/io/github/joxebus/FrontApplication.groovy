package io.github.joxebus

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate

/**
 * This is the main class of the project
 */

@SpringBootApplication(scanBasePackages = ['io.github.joxebus'])
@EnableDiscoveryClient
class FrontApplication {

    static void main(String[] args) {
        SpringApplication.run(FrontApplication, args)
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        new RestTemplate()
    }

}
