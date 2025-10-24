package com.xm.draw2drawbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.api.aliyunai.AliYunAiApi;
import com.xm.draw2drawbackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.xm.draw2drawbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.xm.draw2drawbackend.config.CosClientConfig;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.manager.CosManager;
import com.xm.draw2drawbackend.manager.upload.FilePictureUpload;
import com.xm.draw2drawbackend.manager.upload.PictureUploadTemplate;
import com.xm.draw2drawbackend.manager.upload.UrlPictureUpload;
import com.xm.draw2drawbackend.mapper.PictureMapper;
import com.xm.draw2drawbackend.model.dto.file.UploadPictureResult;
import com.xm.draw2drawbackend.model.dto.picture.*;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.SpaceUser;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.PictureReviewStatusEnum;
import com.xm.draw2drawbackend.model.enums.SpaceRoleEnum;
import com.xm.draw2drawbackend.model.enums.SpaceTypeEnum;
import com.xm.draw2drawbackend.model.vo.PictureVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.PictureService;
import com.xm.draw2drawbackend.service.SpaceService;
import com.xm.draw2drawbackend.service.SpaceUserService;
import com.xm.draw2drawbackend.service.UserService;
import com.xm.draw2drawbackend.utils.ColorSimilarUtils;
import com.xm.draw2drawbackend.utils.ColorTransformUtils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
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
    private SpaceUserService spaceUserService;

    @Resource
    private CosManager cosManager;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private AliYunAiApi aliyunAiApi;

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
     * @param inputSource          文件输入源
     * @param pictureUploadRequest 上传图片参数
     * @param loginUser            登录用户
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在"); // 校验额度
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小不足");
        }
        // 判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null && pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
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
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录 -> 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
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
        // picture.setPicColor(uploadPictureResult.getPicColor());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
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
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
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
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        // and (name like '%searchText%' or introduction like '%searchText%')
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                    .or()
                            .like("introduction", searchText)
            );
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
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
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
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(
                id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum),
                ErrorCode.PARAMS_ERROR, "参数错误");
        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复，已是改状态
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
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
        log.info("开始填充审核参数，图片ID: {}, 用户ID: {}", picture.getId(), loginUser.getId());
        if (userService.isAdmin(loginUser)) {
            // 系统管理员自动过审
            log.info("用户是系统管理员，自动过审");
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("系统管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            log.info("用户不是系统管理员，检查是否为空间管理员");
            // 检查是否为空间管理员
            Long spaceId = picture.getSpaceId();
            // 如果picture对象没有spaceId，从数据库查询原始图片
            if (spaceId == null && picture.getId() != null) {
                Picture originalPicture = this.getById(picture.getId());
                if (originalPicture != null) {
                    spaceId = originalPicture.getSpaceId();
                    log.info("从数据库查询到原始图片的spaceId: {}, pictureId: {}", spaceId, picture.getId());
                }
            }
            log.info("最终使用的spaceId: {}, pictureId: {}", spaceId, picture.getId());
            if (spaceId != null) {
                log.info("图片属于空间，spaceId: {}", spaceId);
                Space space = spaceService.getById(spaceId);
                if (space != null && space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    log.info("是团队空间，检查用户角色");
                    // 团队空间，检查用户是否为空间管理员
                    SpaceUser spaceUser = spaceUserService.lambdaQuery()
                            .eq(SpaceUser::getSpaceId, spaceId)
                            .eq(SpaceUser::getUserId, loginUser.getId())
                            .one();
                    if (spaceUser != null && (SpaceRoleEnum.ADMIN.getValue().equals(spaceUser.getSpaceRole())
                            || SpaceRoleEnum.EDITOR.getValue().equals(spaceUser.getSpaceRole()))) {
                        // 空间管理员和编辑者自动过审
                        String roleText = SpaceRoleEnum.getEnumByValue(spaceUser.getSpaceRole()).getText();
                        log.info("用户是{}，自动过审", roleText);
                        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                        picture.setReviewerId(loginUser.getId());
                        picture.setReviewMessage(roleText + "自动过审");
                        picture.setReviewTime(new Date());
                        return;
                    } else {
                        log.info("用户不是空间管理员或编辑者，spaceUser: {}", spaceUser != null ? spaceUser.getSpaceRole() : "null");
                    }
                } else if (space != null && space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
                    log.info("是私有空间，检查用户是否为空间拥有者");
                    // 私有空间，检查用户是否为空间拥有者
                    if (space.getUserId().equals(loginUser.getId())) {
                        // 私有空间拥有者自动过审
                        log.info("用户是私有空间拥有者，自动过审");
                        picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                        picture.setReviewerId(loginUser.getId());
                        picture.setReviewMessage("私有空间自动过审");
                        picture.setReviewTime(new Date());
                        return;
                    } else {
                        log.info("用户不是私有空间拥有者，spaceUserId: {}, loginUserId: {}", space.getUserId(), loginUser.getId());
                    }
                } else {
                    log.info("未知空间类型或空间不存在，spaceType: {}", space != null ? space.getSpaceType() : "null");
                }
            } else {
                log.info("图片不属于任何空间");
            }
            // 其他情况需要审核
            log.info("设置为待审核状态");
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
        log.info("审核参数填充完成，最终状态: {}, 审核信息: {}", picture.getReviewStatus(), picture.getReviewMessage());
    }

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片参数
     * @param loginUser                   登录用户
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "数量必须大于0");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        // 图片名称前缀，默认值为 "bing_{searchText}_"
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = "bing_" + searchText + "_";
        }

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
        if (ObjUtil.isEmpty(div)) {
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
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?yupi=dog，应该只保留 codefather.cn
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
        // 校验权限(已经改为注解鉴权)
        // checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
                boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
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
        // 权限校验(已经改为注解鉴权)
        // checkPictureAuth(loginUser, oldPicture);
        // 填充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");

        // 调试日志，检查审核状态是否正确设置
        log.info("图片编辑完成，ID: {}, 审核状态: {}, 审核信息: {}",
                picture.getId(), picture.getReviewStatus(), picture.getReviewMessage());
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
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作");
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限操作");
            }
        }
    }

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId   空间ID
     * @param picColor  图片颜色
     * @param loginUser 登录用户
     * @return 图片列表
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR, "参数错误", "颜色参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");
        // 2. 校验空间权限（如果指定了空间ID）
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
        try {
            // 将目标颜色转为 Color 对象
            Color targetColor = Color.decode(picColor.startsWith("#") ? picColor : "#" + picColor);
            // 3. 查询该空间下所有图片（必须有主色调）
            List<Picture> pictureList = this.lambdaQuery()
                    .eq(spaceId != null, Picture::getSpaceId, spaceId)
                    .isNotNull(Picture::getPicColor)
                    .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue())
                    .orderByDesc(Picture::getEditTime)
                    .list();

            // 如果没有图片，直接返回空列表
            if (CollUtil.isEmpty(pictureList)) {
                return Collections.emptyList();
            }
            // 4. 计算相似度并排序，取前12个
            List<Picture> sortedPictures = pictureList.stream()
                    .sorted(Comparator.comparingDouble(picture -> {
                        // 提取图片主色调
                        String hexColor = picture.getPicColor();
                        // 没有主色调的图片放到最后
                        if (StrUtil.isBlank(hexColor)) {
                            return Double.MAX_VALUE;
                        }
                        try {
                            // 确保颜色格式统一
                            if (!hexColor.startsWith("#")) {
                                hexColor = "#" + hexColor;
                            }
                            Color pictureColor = Color.decode(hexColor);
                            // 越大越相似，所以取负值
                            return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                        } catch (NumberFormatException e) {
                            // 颜色格式错误，放到最后
                            return Double.MAX_VALUE;
                        }
                    }))
                    .limit(12)
                    .collect(Collectors.toList());
            // 5. 转换为 PictureVO 并返回结果
            return sortedPictures.stream()
                    .map(PictureVO::objToVo)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色格式不正确，请使用十六进制颜色代码");
        }
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 图片批量编辑参数
     * @param loginUser                 登录用户
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1.获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        // 校验参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(category), ErrorCode.PARAMS_ERROR, "分类不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(tags), ErrorCode.PARAMS_ERROR, "标签列表不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN, "用户未登录");
        // 2.校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceService.checkSpaceAuth(loginUser, space);
        // 3.查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        // 校验图片是否存在
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 4.更新分类和标签
        pictureList.forEach(picture -> {
            // 更新分类和标签
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 5. 命名规则
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        // 6.操作数据库进行批量更新
        boolean result = this.updateBatchById(pictureList);
        // 校验更新是否成功
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片更新失败");
    }

    /**
     * 填充图片列表的名称规则
     * nameRule 图片_{序号} 例如：图片_1
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    /**
     * 创建图片扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建图片扩图任务参数
     * @param loginUser                           登录用户
     * @return 创建结果
     */

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限(已经改为注解鉴权)
        // checkPictureAuth(loginUser, picture);
        // 创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        return aliyunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }
}