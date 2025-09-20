package com.xm.draw2drawbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.model.dto.user.UserQueryRequest;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.LoginUserVO;
import com.xm.draw2drawbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author XMTX8yyds
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-09-18 14:44:18
*/
public interface UserService extends IService<User> {

    /**
     * @param userAccount 账号
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * @param userAccount 账号
     * @param userPassword 用户密码
     * @param request
     * @return 用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户登出
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的登录用户信息     
     * @param user 用户
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息     
     * @param user 用户
     * @return 脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息列表
     * @param userList 用户列表
     * @return 脱敏的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据查询条件构造QueryWrapper
     * @param userQueryRequest 用户查询请求参数
     * @return QueryWrapper<User> 查询包装器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
