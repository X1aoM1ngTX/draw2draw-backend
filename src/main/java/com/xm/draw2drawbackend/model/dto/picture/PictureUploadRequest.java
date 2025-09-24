package com.xm.draw2drawbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片URL
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

}
