package com.xm.draw2drawbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.xm.draw2drawbackend.api.tencentci.model.TencentImageLabelResult;
import com.xm.draw2drawbackend.model.dto.picture.*;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     * @param inputSource 输入源
     * @param pictureUploadRequest 上传图片参数
     * @param loginUser 登录用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest,
            User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片请求
     * @param loginUser 登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
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
     * @param request HTTP请求对象
     * @return Page<PictureVO> 图片VO分页对象
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核参数
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture 图片实体
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 清理图片文件
     *
     * @param oldPicture 旧图片实体
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验图片权限
     *
     * @param loginUser 登录用户
     * @param picture 图片实体
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId 图片ID
     * @param loginUser 登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 图片编辑参数
     * @param loginUser 登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId 空间ID
     * @param picColor 图片颜色
     * @param loginUser 登录用户
     * @return 图片VO列表
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 图片批量编辑参数
     * @param loginUser 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建图片扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建图片扩图任务请求
     * @param loginUser 登录用户
     * @return 任务 ID
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser);

    /**
     * 获取图片标签
     *
     * @param pictureId 图片ID
     * @param bucket 存储桶名称
     * @param key 图片在存储桶中的位置
     * @return 图片标签列表
     */
    List<TencentImageLabelResult> getImageLabels(Long pictureId, String bucket, String key);
}
