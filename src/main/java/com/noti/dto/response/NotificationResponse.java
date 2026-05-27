package com.noti.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String action;
    private String icon;
    private String title;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Date createDate;

    private Boolean seen;
    private Map<String, String> mapExt;
}
