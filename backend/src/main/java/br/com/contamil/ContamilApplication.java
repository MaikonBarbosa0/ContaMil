package br.com.contamil;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContamilApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContamilApplication.class, args);
    }

    @Bean
    Clock applicationClock(@Value("${contamil.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
