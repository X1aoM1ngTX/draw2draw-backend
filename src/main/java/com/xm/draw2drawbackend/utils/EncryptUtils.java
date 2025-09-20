package com.xm.draw2drawbackend.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 加密工具类
 * 基于 Hutool 提供多种加密算法支持
 * 
 * @author xm
 */
@Slf4j
public class EncryptUtils {

    /**
     * 默认AES密钥 - 生产环境请修改为安全的密钥
     */
    private static final String DEFAULT_AES_KEY = "65846813219899856";

    /**
     * 默认盐值 - 生产环境请修改为安全的盐值
     */
    private static final String DEFAULT_SALT = "draw2draw";

    // ==================== 哈希算法 ====================

    /**
     * MD5加密
     * 
     * @param data 待加密数据
     * @return MD5加密结果
     */
    public static String md5(String data) {
        ThrowUtils.throwIf(StrUtil.isBlank(data), ErrorCode.PARAMS_ERROR);
        return DigestUtil.md5Hex(data);
    }

    /**
     * MD5加盐加密
     * 
     * @param data 待加密数据
     * @param salt 盐值
     * @return MD5加盐加密结果
     */
    public static String md5WithSalt(String data, String salt) {
        ThrowUtils.throwIf(StrUtil.isBlank(data), ErrorCode.PARAMS_ERROR);
        String saltedData = data + (StrUtil.isBlank(salt) ? DEFAULT_SALT : salt);
        return DigestUtil.md5Hex(saltedData);
    }

    /**
     * SHA256加密
     * 
     * @param data 待加密数据
     * @return SHA256加密结果
     */
    public static String sha256(String data) {
        ThrowUtils.throwIf(StrUtil.isBlank(data), ErrorCode.PARAMS_ERROR);
        return DigestUtil.sha256Hex(data);
    }

    /**
     * SHA256加盐加密
     * 
     * @param data 待加密数据
     * @param salt 盐值
     * @return SHA256加盐加密结果
     */
    public static String sha256WithSalt(String data, String salt) {
        ThrowUtils.throwIf(StrUtil.isBlank(data), ErrorCode.PARAMS_ERROR);
        String saltedData = data + (StrUtil.isBlank(salt) ? DEFAULT_SALT : salt);
        return DigestUtil.sha256Hex(saltedData);
    }

    // ==================== 对称加密 ====================

    /**
     * AES加密（使用默认密钥）
     * 
     * @param data 待加密数据
     * @return AES加密结果（Base64编码）
     */
    public static String aesEncrypt(String data) {
        return aesEncrypt(data, DEFAULT_AES_KEY);
    }

    /**
     * AES加密
     * 
     * @param data 待加密数据
     * @param key 密钥（16位）
     * @return AES加密结果（Base64编码）
     */
    public static String aesEncrypt(String data, String key) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(data) || StrUtil.isBlank(key), ErrorCode.PARAMS_ERROR);
            AES aes = SecureUtil.aes(key.getBytes(CharsetUtil.CHARSET_UTF_8));
            return aes.encryptBase64(data, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            return "";
        }
    }

    /**
     * AES解密（使用默认密钥）
     * 
     * @param encryptedData 加密数据（Base64编码）
     * @return 解密结果
     */
    public static String aesDecrypt(String encryptedData) {
        return aesDecrypt(encryptedData, DEFAULT_AES_KEY);
    }

    /**
     * AES解密
     * 
     * @param encryptedData 加密数据（Base64编码）
     * @param key 密钥（16位）
     * @return 解密结果
     */
    public static String aesDecrypt(String encryptedData, String key) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(encryptedData) || StrUtil.isBlank(key), ErrorCode.PARAMS_ERROR);
            AES aes = SecureUtil.aes(key.getBytes(CharsetUtil.CHARSET_UTF_8));
            return aes.decryptStr(encryptedData, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            return "";
        }
    }

    /**
     * DES加密
     * 
     * @param data 待加密数据
     * @param key 密钥（8位）
     * @return DES加密结果（Base64编码）
     */
    public static String desEncrypt(String data, String key) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(data) || StrUtil.isBlank(key), ErrorCode.PARAMS_ERROR);
            DES des = SecureUtil.des(key.getBytes(CharsetUtil.CHARSET_UTF_8));
            return des.encryptBase64(data, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("DES加密失败", e);
            return "";
        }
    }

    /**
     * DES解密
     * 
     * @param encryptedData 加密数据（Base64编码）
     * @param key 密钥（8位）
     * @return 解密结果
     */
    public static String desDecrypt(String encryptedData, String key) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(encryptedData) || StrUtil.isBlank(key), ErrorCode.PARAMS_ERROR);
            DES des = SecureUtil.des(key.getBytes(CharsetUtil.CHARSET_UTF_8));
            return des.decryptStr(encryptedData, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("DES解密失败", e);
            return "";
        }
    }

    // ==================== 非对称加密 ====================

    /**
     * 生成RSA密钥对
     * 
     * @return RSA密钥对 [0]私钥 [1]公钥
     */
    public static String[] generateRSAKeyPair() {
        try {
            RSA rsa = SecureUtil.rsa();
            String privateKey = rsa.getPrivateKeyBase64();
            String publicKey = rsa.getPublicKeyBase64();
            return new String[]{privateKey, publicKey};
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
            return new String[]{"", ""};
        }
    }

    /**
     * RSA公钥加密
     * 
     * @param data 待加密数据
     * @param publicKey 公钥
     * @return RSA加密结果（Base64编码）
     */
    public static String rsaEncryptByPublicKey(String data, String publicKey) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(data) || StrUtil.isBlank(publicKey), ErrorCode.PARAMS_ERROR);
            RSA rsa = SecureUtil.rsa(null, publicKey);
            return rsa.encryptBase64(data, cn.hutool.crypto.asymmetric.KeyType.PublicKey);
        } catch (Exception e) {
            log.error("RSA公钥加密失败", e);
            return "";
        }
    }

    /**
     * RSA私钥解密
     * 
     * @param encryptedData 加密数据（Base64编码）
     * @param privateKey 私钥
     * @return 解密结果
     */
    public static String rsaDecryptByPrivateKey(String encryptedData, String privateKey) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(encryptedData) || StrUtil.isBlank(privateKey), ErrorCode.PARAMS_ERROR);
            RSA rsa = SecureUtil.rsa(privateKey, null);
            return rsa.decryptStr(encryptedData, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
        } catch (Exception e) {
            log.error("RSA私钥解密失败", e);
            return "";
        }
    }

    /**
     * RSA私钥加密（用于数字签名）
     * 
     * @param data 待加密数据
     * @param privateKey 私钥
     * @return RSA加密结果（Base64编码）
     */
    public static String rsaEncryptByPrivateKey(String data, String privateKey) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(data) || StrUtil.isBlank(privateKey), ErrorCode.PARAMS_ERROR);
            RSA rsa = SecureUtil.rsa(privateKey, null);
            return rsa.encryptBase64(data, cn.hutool.crypto.asymmetric.KeyType.PrivateKey);
        } catch (Exception e) {
            log.error("RSA私钥加密失败", e);
            return "";
        }
    }

    /**
     * RSA公钥解密（用于验证数字签名）
     * 
     * @param encryptedData 加密数据（Base64编码）
     * @param publicKey 公钥
     * @return 解密结果
     */
    public static String rsaDecryptByPublicKey(String encryptedData, String publicKey) {
        try {
            ThrowUtils.throwIf(StrUtil.isBlank(encryptedData) || StrUtil.isBlank(publicKey), ErrorCode.PARAMS_ERROR);
            RSA rsa = SecureUtil.rsa(null, publicKey);
            return rsa.decryptStr(encryptedData, cn.hutool.crypto.asymmetric.KeyType.PublicKey);
        } catch (Exception e) {
            log.error("RSA公钥解密失败", e);
            return "";
        }
    }

    // ==================== 实用方法 ====================

    /**
     * 生成安全的密码哈希（推荐用于用户密码加密）
     * 
     * @param password 原始密码
     * @return 加密后的密码（SHA256 + 盐值）
     */
    public static String encryptPassword(String password) {
        return sha256WithSalt(password, DEFAULT_SALT);
    }

    /**
     * 验证密码
     * 
     * @param inputPassword 用户输入的密码
     * @param storedPassword 存储的加密密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String inputPassword, String storedPassword) {
        if (StrUtil.isBlank(inputPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        String encryptedInput = encryptPassword(inputPassword);
        return encryptedInput.equals(storedPassword);
    }

    /**
     * 生成随机盐值
     * 
     * @param length 盐值长度
     * @return 随机盐值
     */
    public static String generateSalt(int length) {
        return cn.hutool.core.util.RandomUtil.randomString(length);
    }
}