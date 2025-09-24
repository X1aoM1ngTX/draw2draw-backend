package com.xm.draw2drawbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 爬取图片上传请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 爬取数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;
}
