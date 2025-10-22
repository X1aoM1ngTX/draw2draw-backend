package com.xm.draw2drawbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以色搜图请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 空间ID
     */
    private Long spaceId;
}
