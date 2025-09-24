package com.xm.draw2drawbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis字符串操作测试类
 */
@SpringBootTest
public class RedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testStringOperations() {
        String key = "test:string:key";
        String value = "Hello Redis!";
        String newValue = "Hello Redis Updated!";

        // 1. 测试新增（设置字符串值）
        System.out.println("=== 测试新增 ===");
        stringRedisTemplate.opsForValue().set(key, value);
        String retrievedValue = stringRedisTemplate.opsForValue().get(key);
        System.out.println("设置值: " + value);
        System.out.println("获取值: " + retrievedValue);
        System.out.println("新增测试结果: " + value.equals(retrievedValue));
        System.out.println();

        // 2. 测试修改（更新字符串值）
        System.out.println("=== 测试修改 ===");
        stringRedisTemplate.opsForValue().set(key, newValue);
        String updatedValue = stringRedisTemplate.opsForValue().get(key);
        System.out.println("原值: " + value);
        System.out.println("新值: " + newValue);
        System.out.println("获取更新后的值: " + updatedValue);
        System.out.println("修改测试结果: " + newValue.equals(updatedValue));
        System.out.println();

        // 3. 测试设置过期时间
        System.out.println("=== 测试设置过期时间 ===");
        stringRedisTemplate.opsForValue().set(key + ":expire", value, 10, TimeUnit.SECONDS);
        Long expireTime = stringRedisTemplate.getExpire(key + ":expire");
        System.out.println("设置键 " + key + ":expire 的过期时间为10秒");
        System.out.println("获取剩余过期时间(秒): " + expireTime);
        System.out.println();

        // 4. 测试删除
        System.out.println("=== 测试删除 ===");
        Boolean deleteResult = stringRedisTemplate.delete(key);
        Boolean hasKeyAfterDelete = stringRedisTemplate.hasKey(key);
        System.out.println("删除键: " + key);
        System.out.println("删除结果: " + deleteResult);
        System.out.println("删除后键是否存在: " + hasKeyAfterDelete);
        System.out.println();

        // 5. 测试批量操作
        System.out.println("=== 测试批量操作 ===");
        String batchKey1 = "test:string:batch1";
        String batchKey2 = "test:string:batch2";
        String batchValue1 = "Batch Value 1";
        String batchValue2 = "Batch Value 2";
        
        // 批量设置
        stringRedisTemplate.opsForValue().set(batchKey1, batchValue1);
        stringRedisTemplate.opsForValue().set(batchKey2, batchValue2);
        
        // 批量获取
        String retrievedBatchValue1 = stringRedisTemplate.opsForValue().get(batchKey1);
        String retrievedBatchValue2 = stringRedisTemplate.opsForValue().get(batchKey2);
        
        System.out.println("批量设置值1: " + batchValue1);
        System.out.println("批量获取值1: " + retrievedBatchValue1);
        System.out.println("批量设置值2: " + batchValue2);
        System.out.println("批量获取值2: " + retrievedBatchValue2);
        System.out.println("批量操作测试结果: " + 
            (batchValue1.equals(retrievedBatchValue1) && batchValue2.equals(retrievedBatchValue2)));
        System.out.println();

        // 6. 测试追加操作
        System.out.println("=== 测试追加操作 ===");
        String appendKey = "test:string:append";
        String originalValue = "Original";
        String appendValue = " Appended";
        
        stringRedisTemplate.opsForValue().set(appendKey, originalValue);
        Integer appendResult = stringRedisTemplate.opsForValue().append(appendKey, appendValue);
        String finalValue = stringRedisTemplate.opsForValue().get(appendKey);
        
        System.out.println("原值: " + originalValue);
        System.out.println("追加值: " + appendValue);
        System.out.println("追加后长度: " + appendResult);
        System.out.println("最终值: " + finalValue);
        System.out.println("追加测试结果: " + (originalValue + appendValue).equals(finalValue));
        System.out.println();

        // 清理测试数据
        stringRedisTemplate.delete(key + ":expire");
        stringRedisTemplate.delete(batchKey1);
        stringRedisTemplate.delete(batchKey2);
        stringRedisTemplate.delete(appendKey);
        
        System.out.println("=== 测试完成，已清理测试数据 ===");
    }

    @Test
    public void testSetWithExpire() {
        String key = "test:set:expire";
        String value = "This value will expire in 5 seconds";
        
        // 设置值并指定过期时间
        stringRedisTemplate.opsForValue().set(key, value, 5, TimeUnit.SECONDS);
        
        // 获取值
        String retrievedValue = stringRedisTemplate.opsForValue().get(key);
        Long expireTime = stringRedisTemplate.getExpire(key);
        
        System.out.println("设置带过期时间的键值对");
        System.out.println("键: " + key);
        System.out.println("值: " + retrievedValue);
        System.out.println("过期时间(秒): " + expireTime);
        
        // 清理测试数据
        stringRedisTemplate.delete(key);
    }

    @Test
    public void testSetIfAbsent() {
        String key = "test:set:if:absent";
        String value1 = "First Value";
        String value2 = "Second Value";
        
        // 第一次设置，键不存在，应该设置成功
        Boolean result1 = stringRedisTemplate.opsForValue().setIfAbsent(key, value1);
        String retrievedValue1 = stringRedisTemplate.opsForValue().get(key);
        
        // 第二次设置，键已存在，应该设置失败
        Boolean result2 = stringRedisTemplate.opsForValue().setIfAbsent(key, value2);
        String retrievedValue2 = stringRedisTemplate.opsForValue().get(key);
        
        System.out.println("测试setIfAbsent操作");
        System.out.println("第一次设置结果: " + result1);
        System.out.println("第一次设置后获取的值: " + retrievedValue1);
        System.out.println("第二次设置结果: " + result2);
        System.out.println("第二次设置后获取的值: " + retrievedValue2);
        System.out.println("测试结果: " + (result1 && !result2 && value1.equals(retrievedValue2)));
        
        // 清理测试数据
        stringRedisTemplate.delete(key);
    }
}