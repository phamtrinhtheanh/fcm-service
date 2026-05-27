package com.noti.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BasePageResponse {
    private int page = 1;
    private int limit = 20;
    private int total;
    private int totalPage;

    public BasePageResponse(int page, int limit) {
        this.page = Math.max(page, 1);
        this.limit = Math.min(Math.max(limit, 1), 20);
    }

    public int getTotalPage() {
        if (limit <= 0) return 0;
        return (int) Math.ceil((double) total / limit);
    }
}
