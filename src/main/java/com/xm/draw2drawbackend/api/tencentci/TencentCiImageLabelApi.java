package com.xm.draw2drawbackend.api.tencentci;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.xm.draw2drawbackend.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 腾讯云图像标签识别服务
 */
@Component
@Slf4j
public class TencentCiImageLabelApi {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 识别图片标签
     * @param bucket 图片所在存储桶
     * @param key 图片在存储桶中的位置
     * @return 图片标签识别结果
     */
    public ImageLabelResponse getImageLabel(String bucket, String key) {
        try {
            // 创建请求对象
            ImageLabelRequest request = new ImageLabelRequest();
            
            // 设置请求参数
            request.setBucketName(bucket);
            request.setObjectKey(key);
            
            // 发送请求并返回结果
            // 使用COS的CI处理接口
            ImageLabelResponse response = cosClient.getImageLabel(request);
            log.info("图片标签识别成功，结果: {}", response.getResultJson());
            return response;
        } catch (Exception e) {
            log.error("图片标签识别失败，bucket: {}, key: {}, 错误信息: {}", bucket, key, e.getMessage(), e);
            throw new RuntimeException("图片标签识别失败: " + e.getMessage(), e);
        }
    }
}