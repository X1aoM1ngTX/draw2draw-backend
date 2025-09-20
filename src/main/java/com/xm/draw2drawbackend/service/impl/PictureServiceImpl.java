package com.xm.draw2drawbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.manager.FileManager;
import com.xm.draw2drawbackend.mapper.PictureMapper;
import com.xm.draw2drawbackend.model.dto.file.UploadPictureResult;
import com.xm.draw2drawbackend.model.dto.picture.PictureQueryRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureUploadRequest;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.PictureVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.PictureService;
import com.xm.draw2drawbackend.service.UserService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author X1aoM1ngTX
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-09-20 12:05:51
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;
    
    /**
     * 校验图片参数
     * @param picture 图片实体
     */
    @Override  
    public void validPicture(Picture picture) {  
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);  
        // 从对象中取值  
        Long id = picture.getId();  
        String url = picture.getUrl();  
        String introduction = picture.getIntroduction();  
        // 修改数据时，id 不能为空，有参数则校验  
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "ID不能为空");  
        if (StrUtil.isNotBlank(url)) {  
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "URL过长");  
        }  
        if (StrUtil.isNotBlank(introduction)) {  
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");  
        }  
    }


    /**
     * 上传图片
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 上传图片参数
     * @param loginUser            登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null && pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取查询包装类
     * @param userQueryRequest
     * @return 查询包装类
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();  
        Integer picHeight = pictureQueryRequest.getPicHeight();  
        Double picScale = pictureQueryRequest.getPicScale();  
        String picFormat = pictureQueryRequest.getPicFormat();  
        String searchText = pictureQueryRequest.getSearchText();  
        Long userId = pictureQueryRequest.getUserId();  
        String sortField = pictureQueryRequest.getSortField();  
        String sortOrder = pictureQueryRequest.getSortOrder();  
        // 从多字段中搜索
        // and (name like '%searchText%' or introduction like '%searchText%')
        if (StrUtil.isNotBlank(searchText)) {  
            // 需要拼接查询条件  
            queryWrapper.and(qw -> qw.like("name", searchText)  
                    .or()  
                    .like("introduction", searchText)  
            );  
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);  
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);  
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);  
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);  
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);  
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);  
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);  
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);  
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);  
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);  
        // JSON 数组查询  
        // and tags like '%\"Java\"%' and tags like '%\"TypeScript\"%'
        if (CollUtil.isNotEmpty(tags)) {  
            for (String tag : tags) {  
                queryWrapper.like("tags", "\"" + tag + "\"");  
            }  
        }  
        // 排序  
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);  
        return queryWrapper;  
    }

    /**
     * 获取图片VO
     * @param picture 图片实体
     * @param request HTTP请求对象
     * @return PictureVO 图片VO
     */
    @Override  
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {  
        // 对象转封装类  
        PictureVO pictureVO = PictureVO.objToVo(picture);  
        // 关联查询用户信息  
        Long userId = picture.getUserId();  
        if (userId != null && userId > 0) {  
            User user = userService.getById(userId);  
            UserVO userVO = userService.getUserVO(user);  
            pictureVO.setUser(userVO);  
        }  
        return pictureVO;  
    }

    /**  
    * 分页获取图片封装
    * @param picturePage 图片分页对象
    * @param request HTTP请求对象
    * @return Page<PictureVO> 图片VO分页对象
    */  
    @Override  
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 初始化分页对象
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 空数据检查
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充用户信息到 VO 对象 
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 设置返回结果
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

}
