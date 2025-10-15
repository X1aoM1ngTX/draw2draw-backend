package com.xm.draw2drawbackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}