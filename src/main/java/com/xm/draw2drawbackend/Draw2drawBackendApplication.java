package com.xm.draw2drawbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author X1aoM1ngTX
 */
@SpringBootApplication
@MapperScan("com.xm.draw2drawbackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class Draw2drawBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Draw2drawBackendApplication.class, args);
    }

}
