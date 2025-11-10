package com.gomdol.concert.common.config;

import com.gomdol.concert.queue.application.port.out.QueuePolicyProvider;
import com.gomdol.concert.queue.application.port.out.TokenGenerator;
import com.gomdol.concert.queue.infra.token.Base62TokenGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class QueueConfig {

    @Bean
    public TokenGenerator tokenGenerator(QueueProperties props) {
        return new Base62TokenGenerator(props.tokenLength());
    }

    @Bean
    public QueuePolicyProvider queuePolicyProvider(QueueProperties props) {
        return new QueuePolicyProvider() {
            @Override public long waitingTtlSeconds() { return props.waitingTtlSeconds(); }
            @Override public long enteredTtlSeconds()  { return props.enteredTtlSeconds(); }
            @Override public int capacity() { return props.capacity(); }
//            @Override public int admissionRatePerSec(Long targetId){ return props.admissionPerSec(); }
        };
    }
}
