package com.xm.draw2drawbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.model.dto.AiImageGenerateRequest;
import com.xm.draw2drawbackend.model.dto.AiTaskQueryRequest;
import com.xm.draw2drawbackend.model.entity.AiGenerateTask;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.AiGenerateTaskVO;

public interface AiImageService extends IService<AiGenerateTask> {

    /**
     * 创建AI生成任务
     * @param request AI图片生成请求参数（包含提示词、尺寸等）
     * @param loginUser 当前登录用户
     * @return 任务ID
     */
    String createGenerateTask(AiImageGenerateRequest request, User loginUser);

    /**
     * 根据任务ID查询任务详情，包含生成的图片列表
     * @param taskId 任务ID
     * @param loginUser 当前登录用户
     * @return 任务详情VO
     */
    AiGenerateTaskVO getTaskById(Long taskId, User loginUser);

    /**
     * 分页查询当前用户的AI生成任务列表
     * @param request 查询条件（包含空间ID、状态、模型名等筛选条件）
     * @param loginUser 当前登录用户
     * @return 分页任务列表
     */
    Page<AiGenerateTaskVO> listTasks(AiTaskQueryRequest request, User loginUser);
}
