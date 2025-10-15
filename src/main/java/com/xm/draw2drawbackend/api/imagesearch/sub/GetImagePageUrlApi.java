package com.xm.draw2drawbackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（Step 1）
 */
@Slf4j
public class GetImagePageUrlApi {

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://mms0.baidu.com/it/u=3503787820,2936857853&fm=253&app=138&f=JPEG?w=1422&h=800";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + result);
    }

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
                    .header("Accept-Encoding", "gzip, deflate, br, zstd")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Connection", "keep-alive")
                    .header("acs-token", RandomUtil.randomString(2))
                    .form(formData)
                    .timeout(10000) // 增加超时时间
                    .execute();

            // 只在错误情况下打印响应状态码，正常情况不打印
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                log.info("响应状态码: {}", response.getStatus());
            }

            // 判断响应状态
            ThrowUtils.throwIf(HttpStatus.HTTP_OK != response.getStatus(), ErrorCode.OPERATION_ERROR,
                    "接口调用失败，状态码: " + response.getStatus());

            // 解析响应
            String responseBody = response.body();
            ThrowUtils.throwIf(responseBody == null || responseBody.isEmpty(), ErrorCode.OPERATION_ERROR, "接口返回空响应");

            Map<String, Object> result;
            try {
                result = JSONUtil.toBean(responseBody, Map.class);
            } catch (Exception jsonException) {
                log.error("JSON解析失败，响应内容: {}", responseBody, jsonException);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "响应格式解析失败");
            }

            // 3. 处理响应结果
            ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "接口返回结果为空");

            // 检查状态码
            Object statusObj = result.get("status");
            if (statusObj == null) {
                log.error("API响应中缺少status字段，完整响应: {}", result);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "API响应格式异常，缺少status字段");
            }

            Integer status = null;
            if (statusObj instanceof Integer) {
                status = (Integer) statusObj;
            } else if (statusObj instanceof String) {
                try {
                    status = Integer.valueOf((String) statusObj);
                } catch (NumberFormatException e) {
                    log.error("状态码格式错误: {}", statusObj);
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "API返回的状态码格式错误");
                }
            }

            ThrowUtils.throwIf(status == null || !status.equals(0),
                    ErrorCode.OPERATION_ERROR,
                    "接口调用失败，状态码: " + status);

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            ThrowUtils.throwIf(data == null, ErrorCode.OPERATION_ERROR, "API响应中缺少data字段");

            String rawUrl = (String) data.get("url");
            ThrowUtils.throwIf(rawUrl == null || rawUrl.isEmpty(), ErrorCode.OPERATION_ERROR, "API未返回有效的URL");

            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            ThrowUtils.throwIf(searchResultUrl == null || searchResultUrl.isEmpty(), ErrorCode.OPERATION_ERROR,
                    "URL解码后为空");
            return searchResultUrl;
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("搜索失败，错误详情: ", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败: " + e.getMessage());
        }
    }
}
