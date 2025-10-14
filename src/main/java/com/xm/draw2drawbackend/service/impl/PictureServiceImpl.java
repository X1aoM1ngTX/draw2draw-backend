package com.xm.draw2drawbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.manager.CosManager;
import com.xm.draw2drawbackend.manager.upload.FilePictureUpload;
import com.xm.draw2drawbackend.manager.upload.PictureUploadTemplate;
import com.xm.draw2drawbackend.manager.upload.UrlPictureUpload;
import com.xm.draw2drawbackend.mapper.PictureMapper;
import com.xm.draw2drawbackend.model.dto.file.UploadPictureResult;
import com.xm.draw2drawbackend.model.dto.picture.PictureEditRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureQueryRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureReviewRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureUploadByBatchRequest;
import com.xm.draw2drawbackend.model.dto.picture.PictureUploadRequest;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.PictureReviewStatusEnum;
import com.xm.draw2drawbackend.model.vo.PictureVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.PictureService;
import com.xm.draw2drawbackend.service.SpaceService;
import com.xm.draw2drawbackend.service.UserService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author X1aoM1ngTX
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-09-20 12:05:51
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 校验图片参数
     *
     * @param picture 图片实体
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "ID不能为空");
        // 检查 URL 是否为 null，避免调用 length() 方法时出现空指针异常
        if (url != null) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "URL过长");
        }
        // 检查 introduction 是否为 null，避免调用 length() 方法时出现空指针异常
        if (introduction != null) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 上传图片
     *
     * @param inputSource          文件输入源
     * @param pictureUploadRequest 上传图片参数
     * @param loginUser            登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        // 检验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验是否有空间的权限，仅空间管理员才能上传
            ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH, "无空间访问权限");
            // 校验额度
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小不足");
        }
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null && pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 再次检查 loginUser 是否为 null，防止多线程环境下的竞态条件
            ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NO_AUTH, "用户未登录");
            ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH, "没有空间权限");
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId（这样也兼容了公共图库）
            if (spaceId == null) {
                // 原图片没有空间 id，则继续为 null
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原图片的空间 id 一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 ID 不一致");
                }
            }
        }
        // 上传图片，得到信息
        // 再次检查 loginUser 和其 ID 是否为 null
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NO_AUTH, "用户未登录");
        // 按照用户 id 划分目录 -> 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 私有图库
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型选择不同的上传方式，默认是本地文件上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        // 如果 pictureUploadRequest 中有 picName，则使用它
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 再次检查 loginUser 和其 ID 是否为 null
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NO_AUTH, "用户未登录");
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        Picture oldPicture = null;
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            // 获取旧图片信息，用于后续判断是否需要删除旧图片文件
            oldPicture = this.getById(pictureId);
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        // 如果是更新图片，且图片 URL 发生了变化，需要删除旧的图片文件
        if (oldPicture != null && !StrUtil.equals(oldPicture.getUrl(), picture.getUrl())) {
            // 异步删除旧图片文件
            this.clearPictureFile(oldPicture);
        }
        return PictureVO.objToVo(picture);
    }

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片参数
     * @param loginUser                   登录用户
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 参数校验
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        // 图片名称前缀，默认值为 "bing_{searchText}_"
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = "bing_" + searchText + "_";
        }
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "数量必须大于0");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        log.info("开始批量上传图片，搜索关键词: {}, 数量: {}, 名称前缀: {}", searchText, count, namePrefix);

        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl)
                    .timeout(10000) // 设置10秒超时
                    .get();
        } catch (IOException e) {
            log.error("获取页面失败, URL: " + fetchUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            log.error("获取元素失败，页面结构可能已变化");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败，页面结构可能已变化");
        }

        Elements imgElementList = div.select("img.mimg");
        if (CollUtil.isEmpty(imgElementList)) {
            log.warn("未找到任何图片元素");
            return 0;
        }

        int uploadCount = 0;
        int failCount = 0;
        Set<String> processedUrls = new java.util.HashSet<>(); // 用于去重

        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过");
                continue;
            }

            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 去重处理
            if (processedUrls.contains(fileUrl)) {
                log.info("重复URL，已跳过: {}", fileUrl);
                continue;
            }
            processedUrls.add(fileUrl);

            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(searchText + " " + namePrefix + " " + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}, URL: {}", pictureVO.getId(), fileUrl);
                uploadCount++;
            } catch (BusinessException e) {
                log.error("图片上传失败, URL: " + fileUrl + ", 错误码: " + e.getCode() + ", 错误信息: " + e.getMessage());
                failCount++;
                continue;
            } catch (Exception e) {
                log.error("图片上传失败, URL: " + fileUrl, e);
                failCount++;
                continue;
            }
            // 跳出循环
            if (uploadCount >= count) {
                break;
            }
        }

        log.info("批量上传完成，成功: {}, 失败: {}, 总计: {}", uploadCount, failCount, imgElementList.size());
        return uploadCount;
    }

    /**
     * 获取查询包装类
     *
     * @param pictureQueryRequest 查询参数
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        // and (name like '%searchText%' or introduction like '%searchText%')
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        // JSON 数组查询
        // and tags like '%\"Java\"%' and tags like '%\"TypeScript\"%'
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        // 检查 sortOrder 是否为 null，避免调用 equals 方法时出现空指针异常
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片VO
     *
     * @param picture 图片实体
     * @param request HTTP请求对象
     * @return 图片VO
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 检查 picture 是否为 null
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            // 检查 user 是否为 null，避免传递 null 给 getUserVO 方法
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     *
     * @param picturePage 图片分页对象
     * @param request     HTTP请求对象
     * @return Page<PictureVO> 图片VO分页对象
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 初始化分页对象
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(),
                picturePage.getTotal());
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
            if (userId != null && userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 设置返回结果
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 检查 loginUser 是否为 null
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        ThrowUtils.throwIf(
                id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum),
                ErrorCode.PARAMS_ERROR, "参数错误");
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "青勿重复审核");
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        // 检查 loginUser 的 ID 是否为 null
        ThrowUtils.throwIf(loginUser.getId() == null, ErrorCode.NO_AUTH, "用户ID不能为空");
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     *
     * @param picture   图片实体
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 检查 loginUser 是否为 null
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            // 检查 loginUser 的 ID 是否为 null
            ThrowUtils.throwIf(loginUser.getId() == null, ErrorCode.NO_AUTH, "用户ID不能为空");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 清理图片文件
     *
     * @param oldPicture 旧图片
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        try {
            // 提取路径部分
            String picturePath = new URL(pictureUrl).getPath();
            cosManager.deleteObject(picturePath);

            // 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String thumbnailPath = new URL(thumbnailUrl).getPath();
                cosManager.deleteObject(thumbnailPath);
            }
        } catch (MalformedURLException e) {
            log.error("处理图片删除时遇到格式错误的 URL。图片 URL: {}", pictureUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "格式错误的 URL");
        }
    }

    /**
     * 检查图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
    }

    /**
     * 删除图片
     *
     * @param pictureId 图片ID
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser          登录用户
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将list转为string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 权限校验
        this.checkPictureAuth(loginUser, oldPicture);
        // 填充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");
    }
}
