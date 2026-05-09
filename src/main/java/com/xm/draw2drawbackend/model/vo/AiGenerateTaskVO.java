package com.xm.draw2drawbackend.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AiGenerateTaskVO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 空间ID（null表示公共空间）
     */
    private Long spaceId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 正向提示词
     */
    private String prompt;

    /**
     * 负向提示词
     */
    private String negativePrompt;

    /**
     * 任务状态（PENDING、RUNNING、SUCCESS、FAILED）
     */
    private String status;

    /**
     * 任务状态中文描述
     */
    private String statusText;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 生成图片数量
     */
    private Integer resultCount;

    /**
     * 生成的图片列表
     */
    private List<PictureVO> pictures;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
