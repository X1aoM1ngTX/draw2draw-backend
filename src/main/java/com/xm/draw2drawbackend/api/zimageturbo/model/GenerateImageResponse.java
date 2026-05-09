package com.xm.draw2drawbackend.api.zimageturbo.model;

import lombok.Data;

import java.util.List;

/**
 * DashScope AI图片生成API响应
 */
@Data
public class GenerateImageResponse {

    /**
     * 生成结果输出
     */
    private Output output;

    /**
     * 资源用量统计
     */
    private Usage usage;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 错误码（非null表示请求失败）
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 输出结果
     */
    @Data
    public static class Output {
        /**
         * 生成结果选项列表
         */
        private List<Choice> choices;
    }

    /**
     * 单个生成结果
     */
    @Data
    public static class Choice {
        /**
         * 完成原因
         */
        private String finishReason;
        /**
         * 生成消息内容
         */
        private Message message;
    }

    /**
     * 生成消息
     */
    @Data
    public static class Message {
        /**
         * 角色类型
         */
        private String role;
        /**
         * 内容列表（包含图片和文本）
         */
        private List<Content> content;
    }

    /**
     * 消息内容项（图片URL或文本）
     */
    @Data
    public static class Content {
        /**
         * 生成的图片URL
         */
        private String image;
        /**
         * 文本内容
         */
        private String text;
    }

    /**
     * 资源用量统计
     */
    @Data
    public static class Usage {
        /**
         * 图片宽度
         */
        private Integer width;
        /**
         * 图片高度
         */
        private Integer height;
        /**
         * 生成图片数量
         */
        private Integer imageCount;
        /**
         * 输入token数
         */
        private Integer inputTokens;
        /**
         * 输出token数
         */
        private Integer outputTokens;
        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}
