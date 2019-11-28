# SpringCloudConfig-Bus
SpringCloudConfig-Bus
---
&emsp;&emsp;**[上一篇博客](https://butalways1121.github.io/2019/11/25/SpringCloud%E4%B9%8B%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83Config/)有讲到SpringCloud Refresh机制，但是会发现客户端每次获取最新配置都需要手动进行刷新，如果客户端较少的时候还可以使用，但很当客户端数量较多时要是一个一个去post客户端来刷新配置就很麻烦了，这时，使用Spring Cloud Bus可以完美解决这一问题：只需要在SpringCloud Config Server端发出refresh，就可以触发所有客户端的微服务实现更新。本文的主要内容就是简单介绍SpringCloud Bus的使用，戳[这里](https://github.com/butalways1121/SpringCloudConfig-Bus)下载源码。。**
<!-- more -->
## 一、SpringCloud Bus简介
### 1.SpringCloud Bus是什么
&emsp;&emsp;SpringCloud Bus通过轻量消息代理连接各分布式的节点，它可以用于广播配置文件的更改或者服务之间的通讯，也可以用于监控。SpringCloud Bus的一个核心思想是通过分布式的启动器对SpringBoot应用进行扩展，也可以用来建立一个多应用之间的通信频道。目前唯一实现的方式是用AMQP消息代理作为通道，同样特性的设置（有些取决于通道的设置）在更多通道的文档中。
&emsp;&emsp;SpringCloud Bus被很形象地翻译为“消息总线”，可以将它理解为管理和传播所有分布式项目中的消息，其实本质是利用了MQ的广播机制在分布式的系统中传播消息，目前常用的有Kafka和RabbitMQ。利用Bus的机制可以做很多的事情，其中配置中心客户端刷新就是典型的应用场景之一。
### 2.Bus在配置中心使用的机制
首先，先看一下不使用SpringCloud Bus获取配置信息流程图：
![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/96.png)

再看一下使用SpringCloud Bus获取配置信息流程图:
![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/97.png)

由上图可以看出利用SpringCloud Bus做配置更新的步骤:
&emsp;&emsp;(1)首先在配置中进行更新配置文件信息，提交代码触发post发送bus/refresh；
&emsp;&emsp;(2)Server端接收到请求并发送给SpringCloud Bus；
&emsp;&emsp;(3)SpringCloud Bus接到消息并通知给客户端；
&emsp;&emsp;(4)客户端接收到通知，请求Server端获取最新配置进行更新。
## 二、RabbitMQ的安装与环境配置
&emsp;&emsp;因为我们要用到消息队列MQ，MQ主要使用的是RabbitMQ和Kafka，接下来的示例使用的是RabbitMQ，所以需要安装RabbitMQ并配置相应的环境。先来简单了解一下MQ和RabbitMQ：
&emsp;&emsp;MQ全称为Message Queue, 是一种应用程序对应用程序的通信方法。应用程序通过读写出入队列的消息（针对应用程序的数据）来通信，而无需专用连接来连接它们。消息传递指的是程序之间通过在消息中发送数据进行通信，而不是通过直接调用彼此来通信，直接调用通常是用于诸如远程过程调用的技术。队列的使用除去了接收和发送应用程序同时执行的要求。
&emsp;&emsp;RabbitMQ则是一个实现了AMQP（Advanced Message Queuing Protocol）高级消息队列协议的消息队列服务，使用的是Erlang语言，也因此在安装RabbitMQ之前，需要先安装Erlang并配置相应环境。

### 1.Erlang的安装与配置
&emsp;&emsp;根据电脑系统版本下载Erlang：`http://www.erlang.org/downloads`，下载完成之后进行安装，除了更改安装路径之外其他默认就好，之后系统的高级设置中配置环境变量：
&emsp;&emsp;（1）新建`ERLANG_HOME`，变量值为Erlang的安装目录，例如`D:\Program Files\erl10.5`；
&emsp;&emsp;（2）在Path变量值中加入`;%ERLANG_HOME%\bin`即可。
### 2.RabbitMQ的安装与配置
&emsp;&emsp;同样，根据电脑系统版本下载RabbitMQ：`http://www.rabbitmq.com/install-windows.html `，安装时除了更改安装路径之外其他默认就好，然后在系统的高级设置中配置环境变量：
&emsp;&emsp;（1）新建`RABBITMQ_HOME`，变量值为RabbitMQ的安装目录，例如`D:\Program Files\Rabbit MQServer\rabbitmq_server-3.8.1`；
&emsp;&emsp;（2）在Path变量值中加入`;%RABBITMQ_HOME%\sbin`即可。
### 3.启动RabbitMQ
&emsp;&emsp;配置完成之后，切换到RabbitMQ的sbin目录下，如`D:\Program Files\RabbitMQ Server\rabbitmq_server-3.8.1\sbin`，双击`rabbitmq-server.bat`启动，出现`Starting broker... completed with 3 plugins.`字样则启动成功：

![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/98.png)

**注：如果提示`node with name rabbit already running on *`的错误，就试着删除`C:\Users\Administrator\AppData\Roaming\rabbitmq`目录，再重新启动。如果还是不行，就点击开始菜单，在所有程序 - >RabbitMQ Servr - >RabbitMQ Service - stop，先关闭已经启动的RabbitMQ，然后再重新启动。**
### 4.RabbitMQ的使用
&emsp;&emsp;成功安装配置好RabbitMQ之后，使用官方提供的一个web管理工具rabbitmq_management来对RabbitMQ进行后台管理（安装RabbitMQ自带有该工具）：先切换到RabbitMQ的sbin目录下，输入`rabbitmq-plugins enable rabbitmq_management`进行启动：

![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/99.png)

成功启动后，在浏览器输入`http://localhost:15672/`进行登录，账号密码都是：guest：

![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/100.png)


![](https://raw.githubusercontent.com/butalways1121/img-Blog/master/101.png)

登录成功之后可以使用命令进行用户、角色、权限之类的的配置等等。
## 三、SpringCloud Config Bus示例
### 1.注册中心
&emsp;&emsp;同样，需要先创建一个springcloud-config-bus-eureka项目作为注册中心，为了方便，这里还是贴一下代码吧，**需要注意的是此次的项目最好是使用SpringBoot 2.X、Finchley.SR2版本的，因为使用1.X的话，post后相应的服务会在注册中心掉线，影响后期的调用，但2.X版本的post后短暂掉线后就会重新向注册中心注册**。
pom.xml文件完整配置：
```bash
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>1.0.0</groupId>
	<artifactId>springcloud-config-eureka</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>jar</packaging>
	<name>springcloud-config-eureka</name>
	<url>http://maven.apache.org</url>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.0.6.RELEASE</version>
		<relativePath />
	</parent>
	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
		<java.version>1.8</java.version>
		<spring-cloud.version>Finchley.SR2</spring-cloud.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>${spring-cloud.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
```
application.properties文件：
```bash
spring.application.name=springcloud-config-bus-eureka
server.port=8006
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.client.serviceUrl.defaultZone=http://localhost:8006/eureka/
```
启动类：
```bash
@EnableEurekaServer
@SpringBootApplication
public class App 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class,args);
        System.out.println( "config-bus注册中心服务启动..." );
 
```
### 2.服务端
&emsp;&emsp;创建springcloud-config-bus-server作为服务端，在注册中心的pom.xml基础上添加如下配置，同时应将Eureka的依赖修改为`spring-cloud-starter-netflix-eureka-client`：
```bash
<!-- Config依赖-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-config-server</artifactId>
</dependency>
<!-- 添加监控，以便可以刷新服务端配置-->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- rabbitMQ依赖 -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```
application.properties配置：
```bash
spring.application.name=springcloud-config-bus-server
server.port=9005
eureka.client.serviceUrl.defaultZone=http://localhost:8006/eureka/
# git仓库的地址
 spring.cloud.config.server.git.uri = https://github.com/butalways1121/springcloud-config/
# git仓库地址下的相对地址 多个用逗号","分割
 spring.cloud.config.server.git.search-paths = /config-repo
# git仓库的账号
 spring.cloud.config.server.git.username = 
# git仓库的密码
 spring.cloud.config.server.git.password = 
management.endpoints.web.exposure.include=bus-refresh
#启用springcloud config bus
spring.cloud.bus.enabled = true
#开启跟踪总线事件
spring.cloud.bus.trace.enabled = true
##rabbitmq的地址
spring.rabbitmq.host:127.0.0.1
##rabbitmq的端口
spring.rabbitmq.port:5672
##rabbitmq的用户名
spring.rabbitmq.username:guest
##rabbitmq的密码
spring.rabbitmq.password:guest
```
启动类：
```bash
@EnableEurekaClient
@EnableConfigServer	
@SpringBootApplication
public class App 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class,args);
        System.out.println( "config-bus配置中心服务端启动成功!" );
    }
}
```
### 3.客户端
&emsp;&emsp;此次客户端需要创建两个项目，先创建springcloud-config-bus-clinet，pom.xml在注册中心的pom.xml基础上添加如下配置，同时应将Eureka的依赖修改为`spring-cloud-starter-netflix-eureka-client`：
```bash
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```
bootstrap.properties文件的配置信息:
```bash
spring.cloud.config.name=configtest
spring.cloud.config.profile=pro
spring.cloud.config.label=master
spring.cloud.config.discovery.enabled=true
spring.cloud.config.discovery.serviceId=springcloud-config-bus-server
eureka.client.serviceUrl.defaultZone=http://localhost:8006/eureka/
```
application.properties配置信息：
```bash
spring.application.name=springcloud-config-bus-client2
server.port=9007
management.endpoints.web.exposure.include=refresh
spring.cloud.config.failFast=true
spring.cloud.bus.trace.enabled = true
spring.rabbitmq.host:127.0.0.1
spring.rabbitmq.port:5672
spring.rabbitmq.username:guest
spring.rabbitmq.password:guest
```
提供一个接口供外部调用：
```bash
@RestController
@RefreshScope
public class ClientController {

	@Value("${word}")
	private String word;
	
    @RequestMapping("/hello")
    public String index(@RequestParam String name) {
        return name+","+this.word;
    }
}
```
控制类：
```bash
@EnableEurekaClient
@SpringBootApplication
public class App 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(App.class,args);
        System.out.println( "config-bus配置中心客户端1启动成功!" );
    }
}
```
完成springcloud-config-bus-client的开发之后，将其复制一下，重命名为springcloud-config-bus-client2作为第二个客户端项目，把它的端口改为9007即可。
***
至此，整个项目开发完成。
## 三、测试
&emsp;&emsp;首先启动RabbitMQ服务，接着依次启动springcloud-config-bus-eureka、springcloud-config-bus-server、springcloud-config-bus-client和springcloud-config-bus-client2这四个项目。其中8006是注册中心springcloud-config-bus-eureka的端口，9005是服务端springcloud-config-bus-server的端口，9006是第一个客户端springcloud-config-bus-client的端口，9007是是第二个客户端springcloud-config-bus-client2的端口。接下来的测试分为全局测试和局部测试：

### 1.全局刷新
&emsp;&emsp;启动成功之后，首先在浏览器中输入`http://localhost:9006//hello?name=butalways`来查看服务端configtest-pro.properties的配置信息（此时configtest-pro.properties的内容为`word=hello world!!hello world!!`），浏览器会返回：
```
butalways,hello world!!hello world!!
```
再输入`http://localhost:9007//hello?name=butalways2`，返回：
```
butalways2,hello world!!hello world!!
```
接着，把configtest-pro.properties的配置更改为`word=hello world!!`，然后使用Postman发起post请求`http://localhost:9005/actuator/bus-refresh`，注意这次的地址是服务端的地址和端口，之后在浏览器中输入`http://localhost:9006//hello?name=butalways`，返回：
```
butalways,hello world!!
```
再输入`http://localhost:9007//hello?name=butalways2`，返回：
```
butalways2,hello world!!
```
出现以上结果，则说明全局刷新已经实现了！
### 2.局部刷新
&emsp;&emsp;全局刷新时所有的客户端都会获取到最新的配置，如果只想刷新某一个客户端微服务的配置，就可以使用`http://localhost:9005/actuator/bus-refresh/{destination}`来指定要刷新的应用程序，其中destination就是某一个客户端的服务名，例如，现在我们只想刷新springcloud-config-bus-client2的配置：

&emsp;&emsp;首先，将configtest-pro.properties的配置信息改为`word=hello!!!`，发送post请求`http://localhost:9005/actuator/bus-refresh/springcloud-config-bus-client2`，然后再在浏览器请求`http://localhost:9006//hello?name=butalways`，返回：
```
butalways,hello world!!
```
再请求`http://localhost:9007//hello?name=butalways2`，返回：
```
butalways2,hello!!!
```
以上结果可以看出只有springcloud-config-bus-client2客户端的配置进行了更新，另一个springcloud-config-bus-client并没有刷新，达到了局部刷新的目的。
