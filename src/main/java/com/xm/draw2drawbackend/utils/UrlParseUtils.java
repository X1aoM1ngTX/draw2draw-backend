package com.xm.draw2drawbackend.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * URL解析工具类
 * 用于从COS URL中提取对象键等信息
 *
 * @author X1aoM1ngTX
 */
@Slf4j
public class UrlParseUtils {

    private UrlParseUtils() {
        // 工具类不需要实例化
    }

    /**
     * 从COS URL中提取对象键（包括完整路径）
     * 
     * 支持的URL格式示例：
     * - https://d2d-1370224960.cos.ap-guangzhou.myqcloud.com/public/1968584683583688706/2025-09-20_QYu1weeiG7lPUP4z.png -> public/1968584683583688706/2025-09-20_QYu1weeiG7lPUP4z.png
     * - https://your-bucket.com/public/123456789/1.png -> public/123456789/1.png
     * 
     * @param url COS图片URL
     * @return 对象键（包含完整路径）
     */
    public static String extractObjectKeyFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            log.warn("URL为空，无法提取对象键");
            return "";
        }

        try {
            // 查找协议后的第一个斜杠位置
            int protocolEndIndex = url.indexOf("://");
            if (protocolEndIndex != -1) {
                // 找到域名后的第一个斜杠
                int domainSlashIndex = url.indexOf("/", protocolEndIndex + 3);
                if (domainSlashIndex != -1) {
                    // 提取域名后的完整路径
                    String objectKey = url.substring(domainSlashIndex + 1);
                    log.debug("从URL提取对象键成功: {} -> {}", url, objectKey);
                    return objectKey;
                } else {
                    // 如果没有路径，只返回文件名
                    String fileName = url.substring(url.lastIndexOf("/") + 1);
                    log.debug("URL无路径部分，返回文件名: {} -> {}", url, fileName);
                    return fileName;
                }
            } else {
                // 如果没有协议，直接返回最后一个斜杠后的内容
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                log.debug("URL无协议部分，返回文件名: {} -> {}", url, fileName);
                return fileName;
            }
        } catch (Exception e) {
            log.error("从URL提取对象键失败: {}", url, e);
            // 异常情况下，返回最后一个斜杠后的内容
            return url.substring(url.lastIndexOf("/") + 1);
        }
    }

    /**
     * 从COS URL中提取bucket名称
     * 
     * 支持的URL格式示例：
     * - https://d2d-1370224960.cos.ap-guangzhou.myqcloud.com/public/... -> d2d-1370224960
     * - https://your-bucket.com/public/... -> your-bucket.com
     * 
     * @param url COS图片URL
     * @return bucket名称
     */
    public static String extractBucketFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            log.warn("URL为空，无法提取bucket");
            return "";
        }

        try {
            // 查找协议后的第一个斜杠位置
            int protocolEndIndex = url.indexOf("://");
            if (protocolEndIndex != -1) {
                // 提取协议和域名部分
                String domainPart = url.substring(protocolEndIndex + 3);
                int domainSlashIndex = domainPart.indexOf("/");
                if (domainSlashIndex != -1) {
                    String domain = domainPart.substring(0, domainSlashIndex);
                    
                    // 处理腾讯云COS URL格式: bucket-appid.cos.region.myqcloud.com
                    if (domain.contains(".cos.") && domain.contains(".myqcloud.com")) {
                        // 提取bucket名称（第一个点之前的部分）
                        return domain.split("\\.")[0];
                    }
                    
                    // 其他格式，直接返回域名
                    return domain;
                }
            }
            
            log.warn("无法从URL中提取bucket: {}", url);
            return "";
        } catch (Exception e) {
            log.error("从URL提取bucket失败: {}", url, e);
            return "";
        }
    }

    /**
     * 验证是否为有效的COS URL
     * 
     * @param url 待验证的URL
     * @return 是否为有效的COS URL
     */
    public static boolean isValidCosUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        
        // 基本的URL格式检查
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试腾讯云COS URL解析
        String testUrl = "https://d2d-1370224960.cos.ap-guangzhou.myqcloud.com/public/1968584683583688706/2025-09-20_QYu1weeiG7lPUP4z.png";
        
        String bucket = extractBucketFromUrl(testUrl);
        String objectKey = extractObjectKeyFromUrl(testUrl);
        
        System.out.println("测试URL: " + testUrl);
        System.out.println("提取的bucket: " + bucket);
        System.out.println("提取的objectKey: " + objectKey);
        
        // 预期结果：
        // bucket: d2d-1370224960
        // objectKey: public/1968584683583688706/2025-09-20_QYu1weeiG7lPUP4z.png
    }
}