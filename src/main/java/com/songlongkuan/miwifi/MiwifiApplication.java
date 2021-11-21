package com.songlongkuan.miwifi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MiwifiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiwifiApplication.class, args);
    }

}
