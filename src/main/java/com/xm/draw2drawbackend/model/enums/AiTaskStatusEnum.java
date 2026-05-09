package com.xm.draw2drawbackend.model.enums;

import lombok.Getter;

@Getter
public enum AiTaskStatusEnum {

    PENDING(0, "待处理"),
    RUNNING(1, "生成中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer value;

    private final String text;

    AiTaskStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AiTaskStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AiTaskStatusEnum statusEnum : AiTaskStatusEnum.values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
