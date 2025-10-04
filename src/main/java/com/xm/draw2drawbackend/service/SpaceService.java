package com.xm.draw2drawbackend.service;

import com.xm.draw2drawbackend.model.dto.space.SpaceAddRequest;
import com.xm.draw2drawbackend.model.dto.space.SpaceQueryRequest;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author XMTX8yyds
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-28 22:53:29
 */
public interface SpaceService extends IService<Space> {

    /**
     * 校验
     *
     * @param space 空间
     * @param add   操作类型，true表示创建，false表示修改
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别，自动填充限额
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 添加空间
     *
     * @param spaceAddRequest 添加空间请求
     * @param loginUser       登录用户
     * @return 添加的空间ID
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 获取空间视图
     *
     * @param space   空间
     * @param request 请求
     * @return 空间视图
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间视图列表
     *
     * @param spacePage 空间分页
     * @param request   请求
     * @return 空间视图列表
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
