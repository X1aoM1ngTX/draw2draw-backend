package com.xm.draw2drawbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.manager.AiImageManager;
import com.xm.draw2drawbackend.mapper.AiGenerateTaskMapper;
import com.xm.draw2drawbackend.mapper.PictureMapper;
import com.xm.draw2drawbackend.mapper.SpaceMapper;
import com.xm.draw2drawbackend.mapper.UserMapper;
import com.xm.draw2drawbackend.model.dto.AiImageGenerateRequest;
import com.xm.draw2drawbackend.model.dto.AiTaskQueryRequest;
import com.xm.draw2drawbackend.model.entity.AiGenerateTask;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.AiTaskStatusEnum;
import com.xm.draw2drawbackend.model.vo.AiGenerateTaskVO;
import com.xm.draw2drawbackend.model.vo.PictureVO;
import com.xm.draw2drawbackend.service.AiImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI图片生成服务实现
 */
@Slf4j
@Service
public class AiImageServiceImpl extends ServiceImpl<AiGenerateTaskMapper, AiGenerateTask>
    implements AiImageService {

    @Resource
    private AiImageManager aiImageManager;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private PictureMapper pictureMapper;

    /**
     * 创建AI生成任务，校验配额后异步执行生成
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createGenerateTask(AiImageGenerateRequest request, User loginUser) {
        if (request == null || request.getUserPrompt() == null || request.getUserPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("提示词不能为空");
        }

        if (!aiImageManager.checkUserQuota(loginUser.getId())) {
            throw new IllegalArgumentException("今日生成次数已用完，请明天再试");
        }

        AiGenerateTask task = new AiGenerateTask();
        task.setUserId(loginUser.getId());
        task.setSpaceId(request.getSpaceId());
        task.setModelName("z-image-turbo");
        task.setPrompt(request.getUserPrompt());
        task.setNegativePrompt(request.getNegativePrompt());

        cn.hutool.json.JSONObject paramsJson = new cn.hutool.json.JSONObject();
        paramsJson.set("size", request.getSize() != null ? request.getSize() : "1024*1536");
        paramsJson.set("promptExtend", request.getPromptExtend() != null ? request.getPromptExtend() : false);
        if (request.getSeed() != null) {
            paramsJson.set("seed", request.getSeed());
        }
        task.setParamsJson(paramsJson.toString());

        task.setStatus(AiTaskStatusEnum.PENDING.getValue());
        task.setResultCount(0);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());

        save(task);
        log.info("创建AI生成任务成功，taskId: {}, userId: {}", task.getId(), loginUser.getId());

        aiImageManager.incrementUserQuota(loginUser.getId());

        aiImageManager.executeGenerateTask(task.getId());

        return String.valueOf(task.getId());
    }

    /**
     * 查询任务详情，包含用户名、空间名、生成的图片列表
     */
    @Override
    public AiGenerateTaskVO getTaskById(Long taskId, User loginUser) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("任务ID不能为空");
        }

        AiGenerateTask task = getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        if (!task.getUserId().equals(loginUser.getId())) {
            throw new IllegalArgumentException("无权查看此任务");
        }

        AiGenerateTaskVO vo = new AiGenerateTaskVO();
        BeanUtils.copyProperties(task, vo);

        AiTaskStatusEnum statusEnum = AiTaskStatusEnum.getByValue(task.getStatus());
        vo.setStatus(statusEnum != null ? statusEnum.name() : null);
        vo.setStatusText(statusEnum != null ? statusEnum.getText() : null);

        User user = userMapper.selectById(task.getUserId());
        if (user != null) {
            vo.setUserName(user.getUserName());
        }

        if (task.getSpaceId() != null) {
            Space space = spaceMapper.selectById(task.getSpaceId());
            if (space != null) {
                vo.setSpaceName(space.getSpaceName());
            }
        }

        if (task.getStatus().equals(AiTaskStatusEnum.SUCCESS.getValue())) {
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("aiTaskId", taskId);
            List<Picture> pictures = pictureMapper.selectList(queryWrapper);
            if (pictures != null && !pictures.isEmpty()) {
                List<PictureVO> pictureVOs = pictures.stream().map(picture -> {
                    PictureVO pictureVO = new PictureVO();
                    BeanUtils.copyProperties(picture, pictureVO);
                    return pictureVO;
                }).collect(Collectors.toList());
                vo.setPictures(pictureVOs);
            }
        }

        return vo;
    }

    /**
     * 分页查询当前用户的任务列表，支持按空间、状态、模型筛选
     */
    @Override
    public Page<AiGenerateTaskVO> listTasks(AiTaskQueryRequest request, User loginUser) {
        if (request == null) {
            request = new AiTaskQueryRequest();
        }

        QueryWrapper<AiGenerateTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());

        if (request.getSpaceId() != null) {
            queryWrapper.eq("space_id", request.getSpaceId());
        }

        if (request.getStatus() != null) {
            queryWrapper.eq("status", request.getStatus());
        }

        if (request.getModelName() != null) {
            queryWrapper.eq("model_name", request.getModelName());
        }

        queryWrapper.orderByDesc("create_time");

        Page<AiGenerateTask> page = page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);
        Page<AiGenerateTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setPages(page.getPages());

        List<AiGenerateTaskVO> vos = page.getRecords().stream().map(task -> {
            AiGenerateTaskVO vo = new AiGenerateTaskVO();
            BeanUtils.copyProperties(task, vo);

            AiTaskStatusEnum statusEnum = AiTaskStatusEnum.getByValue(task.getStatus());
            vo.setStatus(statusEnum != null ? statusEnum.name() : null);
            vo.setStatusText(statusEnum != null ? statusEnum.getText() : null);

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }
}
