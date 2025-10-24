package com.xm.draw2drawbackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author X1aoM1ngTX
 */
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.xm.draw2drawbackend.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
public class Draw2drawBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(Draw2drawBackendApplication.class, args);
    }

}
