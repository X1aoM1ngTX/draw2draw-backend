package com.xm.draw2drawbackend.model.dto.picture;

import java.io.Serializable;
import java.util.List;

import com.xm.draw2drawbackend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图片编辑请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {
    
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 搜索词（同时搜名称、简介）
     */
    private String searchText;

    /**
     * 图片作者
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
