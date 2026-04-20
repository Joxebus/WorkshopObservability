package io.github.joxebus

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

/**
 * This is the main class of the project
 */

@SpringBootApplication( scanBasePackages = ['io.github.joxebus'], exclude = [SolrAutoConfiguration])
@EnableDiscoveryClient
class FrontApplication {

    static void main(String[] args) {
        SpringApplication.run(FrontApplication.class, args);
    }

}
