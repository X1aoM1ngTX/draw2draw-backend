package com.xm.draw2drawbackend.manager;

import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.service.PictureService;
import com.xm.draw2drawbackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;

/**
 * 空间图片管理器
 * 用于处理空间和图片的关联操作，避免循环依赖
 *
 * @author X1aoM1ngTX
 */
@Slf4j
@Component
public class SpacePictureManager {

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 删除空间及其关联的图片
     *
     * @param spaceId   空间ID
     * @param loginUser 登录用户
     */
    public void deleteSpaceAndPictures(Long spaceId, User loginUser) {
        ThrowUtils.throwIf(spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");

        // 判断空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 校验权限
        spaceService.checkSpaceAuth(loginUser, space);

        // 开启事务，确保删除操作的原子性
        transactionTemplate.execute(status -> {
            // 查询空间内的所有图片
            List<Picture> pictureList = pictureService.lambdaQuery()
                    .eq(Picture::getSpaceId, spaceId)
                    .list();

            // 删除空间内的所有图片
            if (!pictureList.isEmpty()) {
                for (Picture picture : pictureList) {
                    // 删除图片记录
                    boolean deleteResult = pictureService.removeById(picture.getId());
                    ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR, "删除图片失败");

                    // 异步清理图片文件
                    pictureService.clearPictureFile(picture);
                }
            }

            // 删除空间
            boolean deleteSpaceResult = spaceService.removeById(spaceId);
            ThrowUtils.throwIf(!deleteSpaceResult, ErrorCode.OPERATION_ERROR, "删除空间失败");

            return true;
        });
    }
}