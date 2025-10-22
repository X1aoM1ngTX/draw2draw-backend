package com.xm.draw2drawbackend.api.imagesearch;

import com.xm.draw2drawbackend.api.imagesearch.model.ImageSearchResult;
import com.xm.draw2drawbackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.xm.draw2drawbackend.api.imagesearch.sub.GetImageListApi;
import com.xm.draw2drawbackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImage("https://ts2.tc.mm.bing.net/th/id/OIP-C.RkFmhPVsCKkgN2ACCJZjSQHaEK");
        System.out.println("结果列表" + imageList);
    }

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }
}