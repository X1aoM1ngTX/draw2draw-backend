package com.xm.draw2drawbackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}
