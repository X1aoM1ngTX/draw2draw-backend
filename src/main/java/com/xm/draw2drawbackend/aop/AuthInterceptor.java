package com.xm.draw2drawbackend.aop;

import com.xm.draw2drawbackend.annotation.AuthCheck;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.enums.UserRoleEnum;
import com.xm.draw2drawbackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限拦截器
 *
 * @author X1aoM1ngTX
 */
// 标记此类为切面类，用于实现AOP功能
@Aspect
@Component
public class AuthInterceptor {

    // 使用@Resource注解自动注入UserService实例
    @Resource
    private UserService userService;

    /**
     * 权限拦截器核心方法 - 对带有@AuthCheck注解的方法进行权限验证
     * 使用环绕通知，在目标方法执行前后进行拦截处理
     *
     * @param joinPoint 切入点，包含目标方法的信息
     * @param authCheck 权限校验注解，包含所需的权限信息
     * @return 目标方法的执行结果
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 从注解中获取要求的角色
        String mustRole = authCheck.mustRole();
        // 获取当前请求的属性
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 从请求属性中获取HTTP请求对象
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 通过UserService获取当前登录的用户信息
        User loginUser = userService.getLoginUser(request);
        // 将字符串角色转换为枚举类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 如果注解没有指定角色要求，说明该方法无需权限验证，直接放行执行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 获取当前登录用户的角色枚举
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 如果用户角色不存在，抛出无权限异常
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NO_AUTH);
        // 如果要求管理员权限但用户不是管理员，抛出无权限异常
        ThrowUtils.throwIf(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum),
                ErrorCode.NO_AUTH);
        // 所有权限检查通过，放行执行目标方法
        return joinPoint.proceed();
    }
}
