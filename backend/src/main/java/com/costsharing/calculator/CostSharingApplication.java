package com.costsharing.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用程序主类
 */
@SpringBootApplication
public class CostSharingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostSharingApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("成本分摊计算器后端服务启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================\n");
    }
}
