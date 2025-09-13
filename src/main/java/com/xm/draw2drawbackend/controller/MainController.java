package com.xm.draw2drawbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xm.draw2drawbackend.common.BaseResponse;
import com.xm.draw2drawbackend.common.ResultUtils;

/**
 * 主控制器
 * 
 * @author X1aoM1ngTX
 */
@RestController
@RequestMapping("/")
public class MainController {
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
