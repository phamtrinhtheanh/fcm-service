package com.noti.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class NotificationSearchRequest {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate from;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate to;

    private Integer seen = -1;
    private Integer page = 1;
    private Integer limit = 10;

    public int getOffset() {
        return Math.max(0, (getPage() - 1) * getLimit());
    }

    public Integer getPage() {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    public Integer getLimit() {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
