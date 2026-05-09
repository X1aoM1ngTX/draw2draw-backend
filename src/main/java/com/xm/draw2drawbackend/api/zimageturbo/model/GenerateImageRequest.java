package com.xm.draw2drawbackend.api.zimageturbo.model;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * DashScope Z-Image-Turbo 图片生成请求
 */
@Data
public class GenerateImageRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 输入内容
     */
    private Input input;

    /**
     * 生成参数
     */
    private Parameters parameters;

    /**
     * 输入内容
     */
    @Data
    public static class Input {
        /**
         * 消息列表
         */
        private List<Message> messages;
    }

    /**
     * 对话消息
     */
    @Data
    public static class Message {
        /**
         * 角色（user）
         */
        private String role;
        /**
         * 消息内容列表
         */
        private List<Content> content;
    }

    /**
     * 消息内容
     */
    @Data
    public static class Content {
        /**
         * 提示词文本
         */
        private String text;
    }

    /**
     * 图片生成参数
     */
    @Data
    public static class Parameters {
        /**
         * 图片尺寸，如 "1024*1024"
         */
        private String size;
        /**
         * 是否启用提示词扩展
         */
        private Boolean promptExtend;
        /**
         * 随机种子
         */
        private Integer seed;
    }

    /**
     * 构建图片生成请求
     * @param prompt 正向提示词
     * @param negativePrompt 负向提示词（暂未使用）
     * @param size 图片尺寸
     * @param promptExtend 是否启用提示词扩展
     * @param seed 随机种子
     * @return 构建好的请求对象
     */
    public static GenerateImageRequest create(String prompt, String negativePrompt,
                                               String size, Boolean promptExtend, Integer seed) {
        GenerateImageRequest request = new GenerateImageRequest();
        request.setModel("z-image-turbo");

        Input input = new Input();
        Message message = new Message();
        message.setRole("user");
        Content content = new Content();
        content.setText(prompt);
        message.setContent(Collections.singletonList(content));
        input.setMessages(Collections.singletonList(message));
        request.setInput(input);

        Parameters parameters = new Parameters();
        parameters.setSize(size);
        parameters.setPromptExtend(promptExtend);
        parameters.setSeed(seed);
        request.setParameters(parameters);

        return request;
    }
}
