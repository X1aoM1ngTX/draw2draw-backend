package com.xm.draw2drawbackend.common;

import java.io.Serializable;

import lombok.Data;

/**
 * 通用删除请求
 *
 * @author X1aoM1ngTX
 */
@Data
public class DeleteRequest implements Serializable {
    
    private static final long serialVersionUID = -1L;
    
    /**
     * id
     */
    private Long id;
}
