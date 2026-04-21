package io.github.joxebus.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class InitConfiguration {

    @Value('${rest.consul.root.uri}')
    String rootUri

    @Autowired
    MdcPropagationInterceptor mdcPropagationInterceptor

    @Bean
    @LoadBalanced
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(rootUri)
                .interceptors(mdcPropagationInterceptor)
                .build()
    }

}
