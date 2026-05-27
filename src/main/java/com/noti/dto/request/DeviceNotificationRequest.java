package com.noti.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class DeviceNotificationRequest {
    @NotNull
    private Long receiverID;

    @NotBlank
    private String app;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String action;
    private String icon;
    private String token;
    private Map<String, String> mapExt;
    private Long createDate;
}
