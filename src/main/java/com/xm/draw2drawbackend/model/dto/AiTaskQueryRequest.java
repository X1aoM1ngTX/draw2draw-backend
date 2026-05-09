package com.xm.draw2drawbackend.model.dto;

import com.xm.draw2drawbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI任务查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiTaskQueryRequest extends PageRequest {

    /**
     * 用户ID（可选筛选条件）
     */
    private Long userId;

    /**
     * 空间ID（可选筛选条件）
     */
    private Long spaceId;

    /**
     * 任务状态（可选筛选条件）：0-待处理 1-运行中 2-成功 3-失败
     */
    private Integer status;

    /**
     * 模型名称（可选筛选条件）
     */
    private String modelName;
}
