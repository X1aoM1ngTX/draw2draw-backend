package com.xm.draw2drawbackend.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceRankAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xm.draw2drawbackend.model.dto.space.analyze.SpaceUserAnalyzeRequest;
import com.xm.draw2drawbackend.model.entity.Space;
import com.xm.draw2drawbackend.model.entity.User;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.xm.draw2drawbackend.model.vo.space.analyze.SpaceUserAnalyzeResponse;

/**
 * 空间分析服务
 */
public interface SpaceAnalyzeService extends IService<Space> {

	/**
	 * 获取空间使用情况
	 *
	 * @param spaceAnalyzeRequest 空间分析请求
	 * @param loginUser           登录用户
	 * @return SpaceUsageAnalyzeResponse 空间使用情况
	 */
	SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

	/**
	 * 获取空间分类情况
	 *
	 * @param spaceCategoryAnalyzeRequest 空间分类请求
	 * @param loginUser                   登录用户
	 * @return List<SpaceCategoryAnalyzeResponse> 分类分析结果列表
	 */
	List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
			User loginUser);

	/**
	 * 获取空间标签情况
	 *
	 * @param spaceTagAnalyzeRequest 空间标签请求
	 * @param loginUser              登录用户
	 * @return List<SpaceTagAnalyzeResponse> 标签分析结果列表
	 */
	List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
			User loginUser);

	/**
	 * 获取空间大小情况
	 *
	 * @param spaceSizeAnalyzeRequest 空间大小请求
	 * @param loginUser               登录用户
	 * @return List<SpaceSizeAnalyzeResponse> 大小分析结果列表
	 */
	List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
			User loginUser);

	/**
	 * 获取空间用户情况
	 *
	 * @param spaceUserAnalyzeRequest 空间用户请求
	 * @param loginUser               登录用户
	 * @return List<SpaceUserAnalyzeResponse> 用户分析结果列表
	 */
	List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
			User loginUser);

	/**
	 * 获取空间排行情况
	 *
	 * @param spaceRankAnalyzeRequest 空间排行请求
	 * @param loginUser               登录用户
	 * @return List<Space> 排行分析结果列表
	 */
	List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
