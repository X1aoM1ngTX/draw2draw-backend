package com.xm.draw2drawbackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 以色搜图请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
