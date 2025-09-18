package com.xm.draw2drawbackend.model.dto.user;

import java.io.Serializable;

import lombok.Data;

@Data
public class UserDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
}
