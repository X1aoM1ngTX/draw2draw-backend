package com.xm.draw2drawbackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 文件图片上传实现类
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    /**
     * 校验图片
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long _1MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 5 * _1MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),
                ErrorCode.PARAMS_ERROR,
                "文件类型错误");
    }

    /**
     * 获取原始文件名
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    /**
     * 处理文件
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
