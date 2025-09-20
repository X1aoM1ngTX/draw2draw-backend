package com.xm.draw2drawbackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {
    
    /**
     * 图片ID
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
