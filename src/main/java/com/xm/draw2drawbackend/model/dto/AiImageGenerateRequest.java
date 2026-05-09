package com.xm.draw2drawbackend.model.dto;

import lombok.Data;

import java.util.List;

/**
 * AI图片生成请求
 */
@Data
public class AiImageGenerateRequest {

    /**
     * 用户输入的提示词
     */
    private String userPrompt;

    /**
     * 负向提示词（描述不想生成的内容）
     */
    private String negativePrompt;

    /**
     * 图片尺寸，如 "1024*1024"、"1024*1536"
     */
    private String size;

    /**
     * 是否启用提示词扩展（AI自动补充细节）
     */
    private Boolean promptExtend;

    /**
     * 随机种子（用于复现生成结果）
     */
    private Integer seed;

    /**
     * 所属空间ID（null表示公共空间）
     */
    private Long spaceId;

    /**
     * 图片名称（可选）
     */
    private String pictureName;

    /**
     * 图片分类（可选）
     */
    private String category;

    /**
     * 图片标签列表（可选）
     */
    private List<String> tags;

    /**
     * 图片简介（可选）
     */
    private String introduction;
}
