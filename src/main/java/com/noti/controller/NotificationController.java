package com.noti.controller;

import com.noti.base.BaseListResponse;
import com.noti.base.BaseResponse;
import com.noti.dto.request.DeviceNotificationRequest;
import com.noti.dto.request.NotificationSearchRequest;
import com.noti.dto.response.NotificationResponse;
import com.noti.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${device.subscription.whitelist:evtman}")
    private String appWhitelist;

    @PostMapping("/send")
    public BaseResponse<Void> send(@RequestParam Long userId,
                                   @Valid @RequestBody DeviceNotificationRequest request) throws Exception {
        if (!isAllowedApp(request.getApp())) {
            return BaseResponse.error("Ung dung khong hop le");
        }
        notificationService.publishNotification(request);
        return BaseResponse.success();
    }

    @GetMapping("/search")
    public BaseResponse<BaseListResponse<NotificationResponse>> search(
            @RequestParam Long userId,
            @RequestParam(required = false) String jobTitleCode,
            @Valid NotificationSearchRequest request) {
        var data = notificationService.search(userId, jobTitleCode, request);
        return BaseResponse.success("OK", data);
    }

    @GetMapping("/list")
    public BaseResponse<BaseListResponse<NotificationResponse>> list(
            @RequestParam Long userId,
            @RequestParam(required = false) String jobTitleCode,
            @Valid NotificationSearchRequest request) {
        return search(userId, jobTitleCode, request);
    }

    @GetMapping("/search-by-id")
    public BaseResponse<NotificationResponse> searchById(
            @RequestParam Long userId,
            @RequestParam(required = false) String jobTitleCode,
            @RequestParam("id") Long id,
            @RequestParam("createDate") Long createDate) {
        var res = notificationService.getById(id, userId, jobTitleCode, createDate);
        if (res == null) {
            return new BaseResponse<>(true, "Khong tim thay thong bao");
        }
        return BaseResponse.success("Thanh cong.", res);
    }

    @GetMapping("/{id}")
    public BaseResponse<NotificationResponse> getById(
            @RequestParam Long userId,
            @RequestParam(required = false) String jobTitleCode,
            @PathVariable("id") Long id,
            @RequestParam("createDate") Long createDate) {
        return searchById(userId, jobTitleCode, id, createDate);
    }

    @GetMapping("/unread-count")
    public BaseResponse<Integer> unreadCount(
            @RequestParam Long userId,
            @RequestParam(required = false) String jobTitleCode,
            NotificationSearchRequest request) {
        request.setSeen(0);
        request.setPage(1);
        request.setLimit(1);
        var data = notificationService.search(userId, jobTitleCode, request);
        return BaseResponse.success("OK", data.getPage().getTotal());
    }

    @PutMapping("/seen/{notificationId}")
    public BaseResponse<Void> seen(@RequestParam Long userId,
                                   @PathVariable Long notificationId,
                                   @RequestParam Long createDate) {
        notificationService.markAsSeen(notificationId, userId, createDate);
        return BaseResponse.success();
    }

    private boolean isAllowedApp(String app) {
        if (!StringUtils.hasText(app)) return false;
        Set<String> allowed = Arrays.stream(appWhitelist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return allowed.contains(app);
    }
}
