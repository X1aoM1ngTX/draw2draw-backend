package com.xm.draw2drawbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.model.dto.picture.PictureQueryRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureReviewRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureUploadByBatchRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureUploadRequest;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author X1aoM1ngTX
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-09-20 12:05:51
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片参数
     *
     * @param picture 图片实体
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 上传图片参数
     * @param loginUser            登录用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
            PictureUploadRequest pictureUploadRequest,
            User loginUser);

    /**
     * 批量抓取和创建图片
     * 
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser);

    /**
     * 根据查询条件构造QueryWrapper
     *
     * @param pictureQueryRequest 用户查询请求参数
     * @return QueryWrapper<User> 查询包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片VO
     *
     * @param picture 图片实体
     * @param request HTTP请求对象
     * @return PictureVO 图片VO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     *
     * @param picturePage 图片分页对象
     * @param request     HTTP请求对象
     * @return Page<PictureVO> 图片VO分页对象
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核参数
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture   图片实体
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);
}
