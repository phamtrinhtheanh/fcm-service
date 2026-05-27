package com.noti.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceNotificationEvent {
    private Long receiverID;
    private String app;
    private String title;
    private String content;
    private String action;
    private String icon;
    private Map<String, String> mapExt;
    private Long createDate;
}
