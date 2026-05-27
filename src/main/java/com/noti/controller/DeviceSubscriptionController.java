package com.noti.controller;

import com.noti.base.BaseResponse;
import com.noti.service.DeviceSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/api/device")
@RequiredArgsConstructor
public class DeviceSubscriptionController {

    private final DeviceSubscriptionService subscriptionService;

    @Value("${device.subscription.whitelist:evtman}")
    private String appWhitelist;

    @GetMapping("/subscribe")
    public BaseResponse<Integer> subscribe(@RequestParam Long userId,
                                           @RequestParam String app,
                                           @RequestParam String token) {
        if (!StringUtils.hasText(token)) {
            return new BaseResponse<>(true, "Token khong hop le", 0);
        }
        if (!isAllowed(app)) {
            return new BaseResponse<>(true, "Ung dung khong hop le", 0);
        }
        int result = subscriptionService.subscribe(userId, app, token);
        return BaseResponse.success(result);
    }

    @GetMapping("/unsubscribe")
    public BaseResponse<Integer> unsubscribe(@RequestParam Long userId,
                                              @RequestParam String app) {
        if (!isAllowed(app)) {
            return new BaseResponse<>(true, "Ung dung khong hop le", 0);
        }
        int result = subscriptionService.unsubscribe(userId, app);
        return BaseResponse.success(result);
    }

    private boolean isAllowed(String app) {
        if (!StringUtils.hasText(app)) return false;
        Set<String> allowed = Arrays.stream(appWhitelist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return allowed.contains(app);
    }
}
