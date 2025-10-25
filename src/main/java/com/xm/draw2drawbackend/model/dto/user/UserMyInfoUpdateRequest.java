package com.xm.draw2drawbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己信息的请求
 *
 * @author X1aoM1ngTX
 */
@Data
public class UserMyInfoUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}