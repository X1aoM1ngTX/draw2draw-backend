package com.xm.draw2drawbackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 空间级别值
     */
    private int value;

    /**
     * 空间级别文本
     */
    private String text;

    /**
     * 最大创建数量
     */
    private long maxCount;

    /**
     * 最大空间大小
     */
    private long maxSize;
}
