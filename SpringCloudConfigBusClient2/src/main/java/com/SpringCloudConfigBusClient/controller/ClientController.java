package com.SpringCloudConfigBusClient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



/*
 * 在控制中进行参数的获取，并返回
 * 	springboot 接口测试首先启动 Application 程序，
 * 然后在浏览器输入http://localhost:9006//hello?name=pancm即可查看信息
 */
//@RefreshScope注解表示在接到SpringCloud配置中心配置刷新的时候，自动将新的配置更新到该类对应的字段中
@RestController
@RefreshScope
public class ClientController {


	//@Value注解是默认是从application.properties配置文件获取参数，
	//但是这里我们在客户端并没有进行配置，该配置在配置中心服务端，我们只需指定好了配置文件之后即可进行使用
	@Value("${word}")
	private String word;
	
    @RequestMapping("/hello")
    public String index(@RequestParam String name) {
        return name+","+this.word;
    }
}
