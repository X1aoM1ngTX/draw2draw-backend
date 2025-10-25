package com.xm.draw2drawbackend.model.dto.user;

import lombok.Data;

/**
 * 兑换码
 */
@Data
public class VipCode {

    /**
     * 兑换码
     */
    private String code;

    /**
     * 是否已使用
     */
    private boolean hasUsed;
}