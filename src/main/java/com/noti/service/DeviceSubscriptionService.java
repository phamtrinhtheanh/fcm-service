package com.noti.service;

import com.noti.repository.DeviceSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceSubscriptionService {

    private final DeviceSubscriptionRepository deviceSubscriptionRepository;

    @Transactional
    public int subscribe(Long userId, String app, String token) {
        return deviceSubscriptionRepository.subscribe(userId, app, token);
    }

    @Transactional
    public int unsubscribe(Long userId, String app) {
        return deviceSubscriptionRepository.unsubscribe(userId, app);
    }
}
