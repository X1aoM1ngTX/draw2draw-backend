package com.xm.draw2drawbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xm.draw2drawbackend.exception.BusinessException;
import com.xm.draw2drawbackend.exception.ErrorCode;
import com.xm.draw2drawbackend.exception.ThrowUtils;
import com.xm.draw2drawbackend.mapper.SpaceMapper;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceRankAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceUserAnalyzeRequest;
import com.xm.draw2drawbackend.model.entity.Picture;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceUserAnalyzeResponse;
import com.xm.draw2drawbackend.service.PictureService;
import com.xm.draw2drawbackend.service.SpaceAnalyzeService;
import com.xm.draw2drawbackend.service.SpaceService;
import com.xm.draw2drawbackend.service.UserService;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

/**
 * 空间分析服务实现类
 * 
 * @author XMTX8yyds
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 检查空间分析权限
     * 
     * @param spaceAnalyzeRequest 空间分析请求
     * @param loginUser           登录用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 检查权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH, "没有权访问公共图库");
        } else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID错误");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充空间分析查询条件
     * 
     * @param spaceAnalyzeRequest 空间分析请求
     * @param queryWrapper        查询包装器
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
            QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
            User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 查询全部或公共图库逻辑
            // 仅管理员可以访问
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH, "无权访问空间");
            // 统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0)
                    .sum();
            long usedCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            // 公共图库无上限、无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 查询指定空间
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID错误");
            // 获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 权限校验：仅空间所有者或管理员可访问
            spaceService.checkSpaceAuth(loginUser, space);

            // 构造返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            // 后端直接算好百分比，这样前端可以直接展示
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2)
                    .doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2)
                    .doubleValue();
            response.setCountUsageRatio(countUsageRatio);
            return response;
        }
    }

    /**
     * 获取空间分类分析数据
     *
     * @param spaceCategoryAnalyzeRequest 请求参数
     * @param loginUser                   当前登录用户
     * @return List<SpaceCategoryAnalyzeResponse> 分类分析结果列表
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 根据分析范围补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

        // 使用 MyBatis-Plus 分组查询
        queryWrapper.select("category AS category",
                "COUNT(*) AS count",
                "SUM(picSize) AS totalSize")
                .groupBy("category");

        // 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取空间标签分析数据
     *
     * @param spaceTagAnalyzeRequest 获取空间标签分析数据的请求参数
     * @param loginUser              当前登录用户
     * @return List<SpaceTagAnalyzeResponse> 标签分析结果列表
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
            User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 检查权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        // 查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                // ["Java","C#","Python"], ["Java","C#"] => "Java","C#","Python","Java","C#"
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序排列
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间大小分析数据
     *
     * @param spaceSizeAnalyzeRequest 获取空间大小分析数据的请求参数
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse> 大小分析结果列表
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
            User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());
        // 分组并统计图片大小范围
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<128KB", picSizeList.stream().filter(size -> size < 128 * 1024).count());
        sizeRanges.put("128KB-512KB",
                picSizeList.stream().filter(size -> size >= 128 * 1024 && size < 512 * 1024).count());
        sizeRanges.put("512KB-1MB",
                picSizeList.stream().filter(size -> size >= 512 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间用户分析数据
     *
     * @param spaceUserAnalyzeRequest 获取空间用户分析数据的请求参数
     * @param loginUser               当前登录用户
     * @return List<SpaceUserAnalyzeResponse> 用户分析结果列表
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
            User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.groupBy("date_format(createTime, '%Y-%m-%d') AS period", "count(*) AS count");
                break;
            case "week":
                queryWrapper.groupBy("date_format(createTime, '%Y-%u') AS period", "count(*) AS count");
                break;
            case "month":
                queryWrapper.groupBy("date_format(createTime, '%Y-%m') AS period", "count(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的时间维度: " + timeDimension);
        }

        // 查询所有符合条件的用户ID
        queryWrapper.groupBy("period").orderByAsc("period");
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);

        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取空间排行数据
     *
     * @param spaceRankAnalyzeRequest 获取空间排行数据的请求参数
     * @param loginUser               当前登录用户
     * @return List<Space> 排行数据列表
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH, "无权查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

        // 查询结果
        return spaceService.list(queryWrapper);
    }

}
