package com.xm.draw2drawbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI生成任务实体
 */
@TableName("ai_generate_task")
@Data
public class AiGenerateTask implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 空间ID（null表示公共空间）
     */
    private Long spaceId;

    /**
     * 使用的模型名称
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
     * 生成参数JSON（包含size、promptExtend、seed等）
     */
    private String paramsJson;

    /**
     * 任务状态：0-待处理 1-运行中 2-成功 3-失败
     */
    private Integer status;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;

    /**
     * 生成图片数量
     */
    private Integer resultCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除标记：0-未删除 1-已删除
     */
    @TableLogic
    private Integer isDelete;
}
