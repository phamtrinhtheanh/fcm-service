package com.noti.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.noti.kafka.DeviceNotificationEvent;
import com.noti.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceNotificationSaveConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "${device.notification.kafka.topic}", groupId = "${kafka.consumer.device-notification.group-id}-db")
    public void onNotification(ConsumerRecord<String, String> record) {
        try {
            DeviceNotificationEvent event = mapper.readValue(record.value(), DeviceNotificationEvent.class);
            notificationService.saveNotificationToDb(event);
        } catch (Exception e) {
            log.error("[Kafka-DB] Error: {}", e.getMessage());
        }
    }
}
