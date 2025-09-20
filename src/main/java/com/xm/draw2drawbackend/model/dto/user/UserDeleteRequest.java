package com.xm.draw2drawbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户删除请求
 */
@Data
public class UserDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
}
