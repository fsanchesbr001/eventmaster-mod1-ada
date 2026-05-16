package com.fabriciosanches.ticketservice;

import com.fabriciosanches.ticketservice.config.TicketKafkaTopicsProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableKafka
@SpringBootApplication
@EnableConfigurationProperties(TicketKafkaTopicsProperties.class)
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }

}
