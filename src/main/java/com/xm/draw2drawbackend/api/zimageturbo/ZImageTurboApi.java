package com.xm.draw2drawbackend.api.zimageturbo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xm.draw2drawbackend.api.zimageturbo.model.GenerateImageRequest;
import com.xm.draw2drawbackend.api.zimageturbo.model.GenerateImageResponse;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DashScope Z-Image-Turbo 图片生成API客户端
 */
@Slf4j
@Component
public class ZImageTurboApi {

    /**
     * DashScope多模态生成接口地址
     */
    private static final String GENERATE_URL =
        "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * DashScope API密钥
     */
    @Value("${dashscope.api-key}")
    private String apiKey;

    /**
     * 调用DashScope API生成图片
     * @param request 生成请求参数
     * @return 生成结果响应（包含图片URL）
     * @throws BusinessException 参数为空、HTTP请求失败、内容审核不通过或API返回错误时抛出
     */
    public GenerateImageResponse generateImage(GenerateImageRequest request) {
        ThrowUtils.throwIf(request == null
            || request.getInput() == null
            || request.getInput().getMessages() == null
            || request.getInput().getMessages().isEmpty()
            || StrUtil.isBlank(request.getInput().getMessages().get(0).getContent().get(0).getText()),
            ErrorCode.PARAMS_ERROR, "生成参数为空");

        HttpRequest httpRequest = HttpRequest.post(GENERATE_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request));

        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成失败");
            }
            GenerateImageResponse response = JSONUtil.toBean(httpResponse.body(), GenerateImageResponse.class);
            if (response.getCode() != null) {
                String errorCode = response.getCode();
                String errorMessage = response.getMessage();
                log.error("请求异常：code={}, message={}", errorCode, errorMessage);

                // 处理内容审核错误
                if ("DataInspectionFailed".equals(errorCode)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "提示词包含敏感内容，请修改后重试");
                }

                // 处理其他错误
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成失败，" + errorMessage);
            }
            return response;
        }
    }
}
