package com.gomdol.concert.common.config;

import com.gomdol.concert.reservation.application.port.out.ReservationPolicyProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReservationProperties.class)
public class ReservationConfig {

    @Bean
    public ReservationPolicyProvider reservationPolicyProvider(ReservationProperties props) {
        return new ReservationPolicyProvider() {
            @Override
            public int holdMinutes() {
                return props.holdMinutes();
            }

            @Override
            public int maxSeatsPerReservation() {
                return props.maxSeatsPerReservation();
            }
        };
    }
}
