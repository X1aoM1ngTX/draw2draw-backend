package com.xm.draw2drawbackend.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xm.draw2drawbackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xm.draw2drawbackend.model.entity.SpaceUser;
import com.xm.draw2drawbackend.model.vo.SpaceUserVO;

/**
 * @author XMTX8yyds
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-10-20 12:23:32
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 创建空间成员请求
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser 空间成员
     * @param add       是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser 空间成员
     * @param request   请求
      * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList 空间成员列表
     * @return 空间成员包装类列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest 查询空间成员请求
     * @return 查询对象
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
