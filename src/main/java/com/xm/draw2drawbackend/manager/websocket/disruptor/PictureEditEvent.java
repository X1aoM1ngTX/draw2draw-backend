package com.xm.draw2drawbackend.manager.websocket.disruptor;

import org.springframework.web.socket.WebSocketSession;

import com.xm.draw2drawbackend.manager.websocket.model.PictureEditRequestMessage;
import com.xm.draw2drawbackend.model.entity.User;

import lombok.Data;

/**
 * 图片编辑事件
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
