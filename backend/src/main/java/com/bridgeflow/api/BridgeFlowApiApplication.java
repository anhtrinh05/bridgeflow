package com.bridgeflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BridgeFlowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BridgeFlowApiApplication.class, args);
    }
}
