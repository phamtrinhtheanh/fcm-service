package com.noti.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.noti.dto.request.DeviceNotificationRequest;
import com.noti.kafka.DeviceNotificationEvent;
import com.noti.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceNotificationListener {

    private final NotificationService notificationService;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @KafkaListener(topics = "${device.notification.kafka.topic}", groupId = "${kafka.consumer.device-notification.group-id}-fcm")
    public void onNotification(ConsumerRecord<String, String> record) {
        try {
            DeviceNotificationEvent event = mapper.readValue(record.value(), DeviceNotificationEvent.class);
            String token = notificationService.getActiveToken(event.getReceiverID(), event.getApp());
            if (token != null) {
                DeviceNotificationRequest request = new DeviceNotificationRequest();
                request.setReceiverID(event.getReceiverID());
                request.setApp(event.getApp());
                request.setTitle(event.getTitle());
                request.setContent(event.getContent());
                request.setAction(event.getAction());
                request.setIcon(event.getIcon());
                request.setMapExt(event.getMapExt());
                request.setCreateDate(event.getCreateDate());
                request.setToken(token);
                sendFcm(request);
            }
        } catch (Exception e) {
            log.error("[Kafka-FCM] Error: {}", e.getMessage());
        }
    }

    @Async
    public void sendFcm(DeviceNotificationRequest request) {
        notificationService.sendFcmNotification(request);
    }
}
