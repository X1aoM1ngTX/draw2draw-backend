package com.xm.draw2drawbackend.api.tencentci.model;

import lombok.Data;

/**
 * 腾讯云图片标签识别结果模型
 */
@Data
public class TencentImageLabelResult {

    /**
     * 标签内容
     */
    private String label;

    /**
     * 置信度
     */
    private Float confidence;

    /**
     * 父标签
     */
    private String firstCategory;

    /**
     * 子标签
     */
    private String secondCategory;

    public TencentImageLabelResult(String label, Float confidence, String firstCategory, String secondCategory) {
        this.label = label;
        this.confidence = confidence;
        this.firstCategory = firstCategory;
        this.secondCategory = secondCategory;
    }
}