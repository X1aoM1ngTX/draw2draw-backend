package com.xm.draw2drawbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xm.draw2drawbackend.annotation.AuthCheck;
import com.xm.draw2drawbackend.common.BaseResponse;
import com.xm.draw2drawbackend.common.ResultUtils;
import com.xm.draw2drawbackend.constant.UserConstant;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.manager.upload.FilePictureUpload;
import com.xm.draw2drawbackend.model.dto.file.UploadPictureResult;
import com.xm.draw2drawbackend.model.dto.user.*;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.LoginUserVO;
import com.xm.draw2drawbackend.model.vo.UserVO;
import com.xm.draw2drawbackend.service.UserService;
import com.xm.draw2drawbackend.utils.EncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户控制器
 *
 * @author X1aoM1ngTX
 */
@Api(tags = "User")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    /**
     * 用户注册
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户登出
     */
    @ApiOperation("用户登出")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.OPERATION_ERROR);
        Boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     */
    @ApiOperation("获取当前登录用户")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户添加（管理员）
     */
    @ApiOperation("用户添加（仅管理员）")
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAdddRequest) {
        ThrowUtils.throwIf(userAdddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAdddRequest, user);
        // 默认密码 12345678
        final String DEAFULT_USER_PASSWORD = "12345678";
        String encryptPassword = EncryptUtils.encryptPassword(DEAFULT_USER_PASSWORD);
        user.setUserPassword(encryptPassword);

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "操作注册失败");
        return ResultUtils.success(user.getId());
    }

    /**
     * 获取用户（管理员）
     */
    @ApiOperation("获取用户（仅管理员）")
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long userId) {
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "参数错误");
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @ApiOperation("根据ID获取用户信息（封装类）")
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVoById(long userId) {
        BaseResponse<User> response = getUserById(userId);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户（管理员）
     */
    @ApiOperation("删除用户（仅管理员）")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody UserDeleteRequest userDeleteRequest) {
        ThrowUtils.throwIf(userDeleteRequest == null || userDeleteRequest.getUserId() == null
                || userDeleteRequest.getUserId().isEmpty(), ErrorCode.PARAMS_ERROR, "参数错误");
        long userId = Long.parseLong(userDeleteRequest.getUserId());
        boolean result = userService.removeById(userId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(result);
    }

    /**
     * 更新用户（管理员）
     */
    @ApiOperation("更新用户（仅管理员）")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR,
                "参数错误");
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 用户更新自己的信息
     */
    @ApiOperation("用户更新自己的信息")
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyInfo(@RequestBody UserMyInfoUpdateRequest userMyInfoUpdateRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(userMyInfoUpdateRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");

        // 创建更新对象，只能修改自己的信息
        User user = new User();
        user.setId(loginUser.getId()); // 设置为当前登录用户的ID
        user.setUserName(userMyInfoUpdateRequest.getUserName());
        user.setUserProfile(userMyInfoUpdateRequest.getUserProfile());
        // 注意：用户头像通过单独的上传头像接口修改，这里不包含

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败");

        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（除管理员）
     */
    @ApiOperation("分页获取用户列表")
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 上传用户头像
     */
    @ApiOperation("上传用户头像")
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadUserAvatar(
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");

        // 上传头像，按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/avatar/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(multipartFile, uploadPathPrefix);

        // 使用UploadPictureResult中的缩略图URL（如果存在）
        String finalAvatarUrl = uploadPictureResult.getThumbnailUrl();
        // 如果缩略图URL不存在，则使用原图URL
        if (finalAvatarUrl == null || finalAvatarUrl.isEmpty()) {
            finalAvatarUrl = uploadPictureResult.getUrl();
        }

        // 更新用户头像
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserAvatar(finalAvatarUrl);
        boolean result = userService.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "头像更新失败");

        return ResultUtils.success(finalAvatarUrl);
    }

    /**
     * 兑换会员
     */
    @ApiOperation("兑换会员")
    @PostMapping("/exchange/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipExchangeRequest vipExchangeRequest,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(vipExchangeRequest == null, ErrorCode.PARAMS_ERROR);
        String vipCode = vipExchangeRequest.getVipCode();
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 调用 service 层的方法进行会员兑换
        boolean result = userService.exchangeVipByCode(loginUser, vipCode);
        return ResultUtils.success(result);
    }

}
