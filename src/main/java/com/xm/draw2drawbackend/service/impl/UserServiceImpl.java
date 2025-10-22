package com.xm.draw2drawbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.constant.UserConstant;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.manager.auth.StpKit;
import com.xm.draw2drawbackend.mapper.UserMapper;
import com.xm.draw2drawbackend.model.dto.user.UserQueryRequest;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.UserRoleEnum;
import com.xm.draw2drawbackend.model.vo.LoginUserVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.UserService;
import com.xm.draw2drawbackend.utils.EncryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author X1aoM1ngTX
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-09-18 14:44:18
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 用户ID
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        // 2. 验证账号和密码
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        // 3. 验证两次输入的密码一致
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.USER_PASSWORD_ERROR, "两次输入的密码不一致");
        // 4. 验证账号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        ThrowUtils.throwIf(this.baseMapper.selectCount(queryWrapper) > 0, ErrorCode.USER_ALREADY_EXIST, "账号已存在");
        // 5. 加密密码
        String encryptPassword = EncryptUtils.encryptPassword(userPassword);
        // 6. 插入用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("用户");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        // 7. 返回用户ID
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 用户密码
     * @param request      Http请求
     * @return 用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        // 2. 验证账号和密码
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        // 3. 加密传递过来的密码
        String encryptPassword = EncryptUtils.encryptPassword(userPassword);
        // 4. 验证账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("用户登录失败！账号 {} 不存在或密码错误", userAccount);
            ThrowUtils.throwIf(true, ErrorCode.USER_ACCOUNT_NOT_EXIST, "用户不存在或者密码错误");
        }
        // 5. 记录用户登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 6. 返回脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request Http请求
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN, "用户未登录");
        // 从数据库查询（追求性能的话可以注释，直接走缓存,但是有可能不是最新数据）
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN, "用户未登录");
        return currentUser;
    }

    /**
     * 用户登出
     *
     * @param request Http请求
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.OPERATION_ERROR, "用户未登录");
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户转换参数为空");
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return 用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户转换参数为空");
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(user -> getUserVO(user))
                .collect(Collectors.toList());
    }

    /**
     * 获取查询包装类
     *
     * @param userQueryRequest
     * @return 查询包装类
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "用户转换参数为空");
        // 创建查询条件
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 创建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @param user 用户
     * @return true: 管理员，false: 不是管理员
     */
    @Override
    public boolean isAdmin(User user) {
        // 返回是否为管理员
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}