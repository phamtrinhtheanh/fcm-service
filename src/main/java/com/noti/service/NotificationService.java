package com.noti.service;

import com.noti.base.BaseListResponse;
import com.noti.base.BasePageResponse;
import com.noti.dto.request.DeviceNotificationRequest;
import com.noti.dto.request.NotificationSearchRequest;
import com.noti.dto.response.NotificationResponse;
import com.noti.kafka.DeviceNotificationEvent;
import com.noti.repository.DeviceSubscriptionRepository;
import com.noti.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceSubscriptionRepository deviceSubscriptionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    @Value("${device.notification.kafka.topic:device-notification-events}")
    private String topic;

    public void publishNotification(DeviceNotificationRequest request) throws Exception {
        DeviceNotificationEvent event = DeviceNotificationEvent.builder()
                .receiverID(request.getReceiverID())
                .app(request.getApp())
                .title(request.getTitle())
                .content(request.getContent())
                .action(request.getAction())
                .icon(request.getIcon())
                .mapExt(request.getMapExt())
                .createDate(System.currentTimeMillis())
                .build();

        String key = request.getReceiverID() + "-" + request.getApp();
        kafkaTemplate.send(topic, key, event).get();
        log.info("[Noti] Published event: receiverID={}, app={}", request.getReceiverID(), request.getApp());
    }

    public String getActiveToken(Long receiverID, String app) {
        return deviceSubscriptionRepository.getActiveToken(receiverID, app);
    }

    public String sendFcmNotification(DeviceNotificationRequest request) {
        String token = request.getToken();
        if (!StringUtils.hasText(token)) {
            log.warn("[FCM] No device token for receiverID={}, app={}", request.getReceiverID(), request.getApp());
            return "no-device-token";
        }

        FirebaseMessaging fcm = firebaseMessagingProvider.getIfAvailable();
        if (fcm == null) {
            log.warn("[FCM] Firebase unavailable");
            return "firebase-unavailable";
        }

        Map<String, String> data = new HashMap<>();
        if (request.getMapExt() != null) data.putAll(request.getMapExt());
        if (StringUtils.hasText(request.getAction())) data.put("action", request.getAction());
        if (StringUtils.hasText(request.getIcon())) data.put("icon", request.getIcon());
        if (StringUtils.hasText(request.getApp())) data.put("app", request.getApp());
        if (request.getCreateDate() != null) data.put("createDate", String.valueOf(request.getCreateDate()));
        data.put("receiverID", String.valueOf(request.getReceiverID()));

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getContent())
                        .build())
                .putAllData(data)
                .build();

        try {
            return fcm.send(message);
        } catch (Exception e) {
            log.error("[FCM] Send failed: {}", e.getMessage());
            return "firebase-send-failed";
        }
    }

    public void saveNotificationToDb(DeviceNotificationEvent event) {
        Long id = notificationRepository.insert(event);
        log.info("[Noti] Saved to DB: id={}, receiverID={}", id, event.getReceiverID());
    }

    public void markAsSeen(Long notificationId, Long userId, Long createDate) {
        notificationRepository.markAsSeen(notificationId, String.valueOf(userId), createDate);
    }

    private LocalDateTime[] resolveDateBoundary(NotificationSearchRequest request) {
        LocalDate fromDate = request.getFrom();
        LocalDate toDate = request.getTo();

        if (toDate == null) {
            if (fromDate == null) {
                fromDate = LocalDate.now().minusDays(7);
                toDate = LocalDate.now();
            } else {
                LocalDate maxEnd = fromDate.plusDays(7);
                toDate = maxEnd.isAfter(LocalDate.now()) ? LocalDate.now() : maxEnd;
            }
        } else {
            if (fromDate == null) {
                fromDate = toDate.minusDays(7);
            } else if (ChronoUnit.DAYS.between(fromDate, toDate) > 14) {
                fromDate = toDate.minusDays(14);
            }
        }

        return new LocalDateTime[]{
                fromDate.atStartOfDay(),
                toDate.atTime(LocalTime.MAX)
        };
    }

    public BaseListResponse<NotificationResponse> search(Long userId, String jobCode, NotificationSearchRequest request) {
        String userIdStr = String.valueOf(userId);
        LocalDateTime[] dates = resolveDateBoundary(request);

        List<NotificationResponse> list = notificationRepository.search(userIdStr, jobCode, dates[0], dates[1], request);
        int total = notificationRepository.countTotal(userIdStr, jobCode, dates[0], dates[1], request);

        BasePageResponse page = new BasePageResponse(request.getPage(), request.getLimit());
        page.setTotal(total);

        BaseListResponse<NotificationResponse> result = new BaseListResponse<>();
        result.setData(list);
        result.setPage(page);
        return result;
    }

    public NotificationResponse getById(Long notificationId, Long userId, String jobCode, Long createDate) {
        return notificationRepository.getById(
                notificationId,
                String.valueOf(userId),
                jobCode,
                createDate
        ).orElse(null);
    }
}
