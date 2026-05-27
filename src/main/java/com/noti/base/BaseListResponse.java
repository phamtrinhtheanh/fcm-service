package com.noti.base;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BaseListResponse<T> {
    private BasePageResponse page;
    private List<T> data;
}
