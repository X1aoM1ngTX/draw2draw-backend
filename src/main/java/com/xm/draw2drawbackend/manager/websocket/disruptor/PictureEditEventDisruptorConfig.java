package com.xm.draw2drawbackend.manager.websocket.disruptor;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lmax.disruptor.dsl.Disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;

/**
 * 图片编辑事件 Disruptor 配置
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    /**
     * 创建 disruptor
     */
    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 启动 disruptor
        disruptor.start();
        return disruptor;
    }
}
