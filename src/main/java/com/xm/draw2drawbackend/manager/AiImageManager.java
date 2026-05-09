package com.xm.draw2drawbackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.xm.draw2drawbackend.api.zimageturbo.ZImageTurboApi;
import com.xm.draw2drawbackend.api.zimageturbo.model.GenerateImageRequest;
import com.xm.draw2drawbackend.api.zimageturbo.model.GenerateImageResponse;
import com.xm.draw2drawbackend.mapper.AiGenerateTaskMapper;
import com.xm.draw2drawbackend.mapper.PictureMapper;
import com.xm.draw2drawbackend.model.entity.AiGenerateTask;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.enums.AiTaskStatusEnum;
import com.xm.draw2drawbackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AiImageManager {

    @Resource
    private ZImageTurboApi zImageTurboApi;

    @Resource
    private CosManager cosManager;

    @Resource
    private AiGenerateTaskMapper aiGenerateTaskMapper;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Value("${cos.client.bucket}")
    private String bucket;

    @Value("${cos.client.region}")
    private String region;

    private static final String COS_HOST = "https://%s.cos.%s.myqcloud.com";
    private static final int DAILY_QUOTA = 10;

    @Async("taskExecutor")
    public void executeGenerateTask(Long taskId) {
        log.info("开始执行AI生成任务，taskId: {}", taskId);

        AiGenerateTask task = new AiGenerateTask();
        task.setId(taskId);
        task.setStatus(AiTaskStatusEnum.RUNNING.getValue());
        task.setUpdateTime(new java.util.Date());

        aiGenerateTaskMapper.updateById(task);

        try {
            AiGenerateTask taskInfo = aiGenerateTaskMapper.selectById(taskId);
            GenerateImageRequest apiRequest = GenerateImageRequest.create(
                taskInfo.getPrompt(),
                taskInfo.getNegativePrompt(),
                JSONUtil.parseObj(taskInfo.getParamsJson()).getStr("size"),
                JSONUtil.parseObj(taskInfo.getParamsJson()).getBool("promptExtend"),
                JSONUtil.parseObj(taskInfo.getParamsJson()).getInt("seed")
            );

            log.info("调用DashScope API，taskId: {}, prompt: {}", taskId, apiRequest.getInput().getMessages().get(0).getContent().get(0).getText());
            GenerateImageResponse response = zImageTurboApi.generateImage(apiRequest);

            if (response != null && response.getOutput() != null 
                && response.getOutput().getChoices() != null 
                && !response.getOutput().getChoices().isEmpty()) {
                
                GenerateImageResponse.Choice choice = response.getOutput().getChoices().get(0);
                if (choice.getMessage() != null && choice.getMessage().getContent() != null 
                    && !choice.getMessage().getContent().isEmpty()) {
                    
                    String imageUrl = choice.getMessage().getContent().get(0).getImage();
                    log.info("获取到图片URL，taskId: {}, imageUrl: {}", taskId, imageUrl);

                    File tempFile = null;
                    try {
                        tempFile = downloadImage(imageUrl, taskId);
                        String cosKey = generateCosKey(taskInfo.getUserId(), taskId);

                        PutObjectResult putResult = cosManager.putPictureObject(cosKey, tempFile);
                        log.info("图片上传COS成功，taskId: {}, cosKey: {}", taskId, cosKey);

                        String cosUrl = String.format(COS_HOST, bucket, region) + "/" + cosKey;
                        createPicture(taskInfo, cosUrl, imageUrl);

                        task.setStatus(AiTaskStatusEnum.SUCCESS.getValue());
                        task.setResultCount(1);
                    } finally {
                        if (tempFile != null && tempFile.exists()) {
                            FileUtil.del(tempFile);
                        }
                    }
                } else {
                    throw new RuntimeException("未获取到生成的图片");
                }
            } else {
                throw new RuntimeException("API响应异常");
            }
        } catch (Exception e) {
            log.error("AI生成任务执行失败，taskId: {}", taskId, e);
            task.setStatus(AiTaskStatusEnum.FAILED.getValue());
            task.setErrorMessage(e.getMessage());
        } finally {
            task.setUpdateTime(new java.util.Date());
            aiGenerateTaskMapper.updateById(task);
        }
    }

    private File downloadImage(String imageUrl, Long taskId) throws Exception {
        String suffix = ".png";
        URL url = new URL(imageUrl);
        String path = url.getPath();
        if (StrUtil.isNotBlank(path)) {
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex > 0) {
                suffix = path.substring(lastDotIndex);
            }
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "ai_image");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, taskId + suffix);
        HttpUtil.downloadFile(imageUrl, tempFile);
        return tempFile;
    }

    private String generateCosKey(Long userId, Long taskId) {
        return String.format("ai/%s/%s.png", userId, IdUtil.simpleUUID());
    }

    private void createPicture(AiGenerateTask task, String cosUrl, String originalImageUrl) {
        Picture picture = new Picture();
        picture.setUrl(cosUrl);
        picture.setName("AI生成图片_" + task.getId());
        picture.setIntroduction(task.getPrompt());
        picture.setUserId(task.getUserId());
        picture.setSpaceId(task.getSpaceId());
        picture.setSourceType("AI_GENERATED");
        picture.setAiTaskId(task.getId());

        JSONObject metadata = new JSONObject();
        metadata.set("prompt", task.getPrompt());
        if (StrUtil.isNotBlank(task.getNegativePrompt())) {
            metadata.set("negativePrompt", task.getNegativePrompt());
        }
        metadata.set("params", JSONUtil.parseObj(task.getParamsJson()));
        metadata.set("model", task.getModelName());
        metadata.set("originalImageUrl", originalImageUrl);
        picture.setMetadataJson(metadata.toString());

        pictureMapper.insert(picture);
        log.info("创建picture记录成功，pictureId: {}", picture.getId());
    }

    public boolean checkUserQuota(Long userId) {
        String key = String.format("ai:generate:count:%d:%s", userId, LocalDate.now());
        String count = stringRedisTemplate.opsForValue().get(key);
        return count == null || Integer.parseInt(count) < DAILY_QUOTA;
    }

    public void incrementUserQuota(Long userId) {
        String key = String.format("ai:generate:count:%d:%s", userId, LocalDate.now());
        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 2, TimeUnit.DAYS);
    }
}
