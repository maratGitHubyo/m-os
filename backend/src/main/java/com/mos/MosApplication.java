package com.mos;

import com.mos.qrcode.config.MosQrProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MosQrProperties.class)
@EnableScheduling
public class MosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MosApplication.class, args);
    }
}
