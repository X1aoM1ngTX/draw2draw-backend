package com.xm.draw2drawbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xm.draw2drawbackend.common.BaseResponse;
import com.xm.draw2drawbackend.common.ResultUtils;
import com.xm.draw2drawbackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.xm.draw2drawbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xm.draw2drawbackend.model.dto.AiImageGenerateRequest;
import com.xm.draw2drawbackend.model.dto.AiTaskQueryRequest;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.AiGenerateTaskVO;
import com.xm.draw2drawbackend.service.AiImageService;
import com.xm.draw2drawbackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * AI图片生成控制器
 */
@Api(tags = "AI图片生成")
@RestController
@RequestMapping("/ai/image")
@Slf4j
public class AiImageController {

    @Resource
    private AiImageService aiImageService;

    @Resource
    private UserService userService;

    /**
     * 创建AI图片生成任务
     */
    @ApiOperation("创建AI生成任务")
    @PostMapping("/generate")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<String> generateImage(@RequestBody AiImageGenerateRequest request,
                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        String taskId = aiImageService.createGenerateTask(request, loginUser);
        return ResultUtils.success(taskId);
    }

    /**
     * 根据任务ID查询生成任务详情
     */
    @ApiOperation("查询任务详情")
    @GetMapping("/task/{taskId}")
    public BaseResponse<AiGenerateTaskVO> getTask(@PathVariable String taskId,
                                                   HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long taskIdLong = Long.parseLong(taskId);
        AiGenerateTaskVO task = aiImageService.getTaskById(taskIdLong, loginUser);
        return ResultUtils.success(task);
    }

    /**
     * 分页查询当前用户的AI生成任务列表
     */
    @ApiOperation("分页查询任务列表")
    @PostMapping("/tasks")
    public BaseResponse<Page<AiGenerateTaskVO>> listTasks(@RequestBody AiTaskQueryRequest request,
                                                          HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<AiGenerateTaskVO> tasks = aiImageService.listTasks(request, loginUser);
        return ResultUtils.success(tasks);
    }
}
