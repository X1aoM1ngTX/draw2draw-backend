package com.xm.draw2drawbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.mapper.SpaceMapper;
import com.xm.draw2drawbackend.model.dto.space.SpaceAddRequest;
import com.xm.draw2drawbackend.model.dto.space.SpaceQueryRequest;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.SpaceLevelEnum;
import com.xm.draw2drawbackend.model.vo.SpaceVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.SpaceService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.xm.draw2drawbackend.service.UserService;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author XMTX8yyds
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-28 22:53:29
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    // 使用 ConcurrentHashMap 存储锁对象，防止内存泄漏
    private static final Map<Long, Object> LOCK_MAP = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 校验空间
     *
     * @param space 空间
     * @param add   是否创建
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            ThrowUtils.throwIf(spaceName == null, ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
        }
        // 修改数据时，如果要改空间级别
        ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        // 空间名称不为空且过长
        ThrowUtils.throwIf(spaceName != null && spaceName.length() > 30,
                ErrorCode.PARAMS_ERROR, "空间名称过长");
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest 空间添加请求
     * @param loginUser       登录用户
     * @return long 新空间ID
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        // 转换实体类和 DTO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.FREE.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        this.validSpace(space, true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.FREE.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限创建指定级别的空间");
        }
        // 4. 控制同一用户只能创建一个私有空间
        Object lock = LOCK_MAP.computeIfAbsent(userId, key -> new Object());
        try {
            synchronized (lock) {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    // 判断是否已有空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .exists();
                    // 如果已有空间，就不能再创建
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                    // 创建
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                    // 返回新写入的数据 id
                    return space.getId();
                });
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            }
        } finally {
            // 防止内存泄漏
            LOCK_MAP.remove(userId);
        }
    }

    /**
     * 获取空间VO
     *
     * @param space   空间实体
     * @param request HTTP请求对象
     * @return 空间VO
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 检查 space 是否为 null
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            // 检查 user 是否为 null，避免传递 null 给 getUserVO 方法
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 分页获取空间封装
     *
     * @param spacePage 空间分页对象
     * @param request   HTTP请求对象
     * @return Page<spacePage> 空间VO分页对象
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 初始化分页对象
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(),
                spacePage.getTotal());
        // 空数据检查
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充用户信息到 VO 对象
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userId != null && userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        // 设置返回结果
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 获取查询包装类
     *
     * @param spaceQueryRequest 查询参数
     * @return 查询包装类
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);

        // 排序
        // 检查 sortOrder 是否为 null，避免调用 equals 方法时出现空指针异常
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 根据空间级别，自动填充限额
     *
     * @param space 空间
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            // 如果前端没有传 maxSize，则自动填充
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            // 如果前端没有传 maxCount，则自动填充
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 校验空间权限
     *
     * @param loginUser 登录用户
     * @param space     空间
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "无权限操作该空间");
    }
}
