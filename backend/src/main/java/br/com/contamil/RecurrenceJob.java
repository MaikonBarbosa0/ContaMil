package br.com.contamil;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class RecurrenceJob {
    private final RecurrenceService service;

    RecurrenceJob(RecurrenceService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        service.generatePending();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "${contamil.time-zone}")
    public void atMidnight() {
        service.generatePending();
    }
}
