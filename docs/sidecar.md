# 使用Sidecar将Node.js引入Spring Cloud

## 理论

### 简介

Spring Cloud是目前非常流行的微服务化解决方案，它将Spring Boot的便捷开发和Netflix OSS的丰富解决方案结合起来。如我们所知，Spring Cloud不同于Dubbo，使用的是基于HTTP(s)的Rest服务来构建整个服务体系。

那么有没有可能使用一些非JVM语言，例如我们所熟悉的Node.js来开发一些Rest服务呢？当然是可以的。但是如果只有Rest服务，还不能接入Spring Cloud系统。我们还想使用起Spring Cloud提供的Eureka进行服务发现，使用Config Server做配置管理，使用Ribbon做客户端负载均衡。这个时候Spring sidecar就可以大显身手了。

Sidecar起源于[Netflix Prana](https://github.com/Netflix/Prana)。他提供一个可以获取既定服务所有实例的信息(例如host，端口等)的http api。你也可以通过一个嵌入的Zuul，代理服务到从Eureka获取的相关路由节点。Spring Cloud Config Server可以直接通过主机查找或通过代理Zuul进行访问。

需要注意的是你所开发的Node.js应用，必须去实现一个健康检查接口，来让Sidecar可以把这个服务实例的健康状况报告给Eureka。

为了使用Sidecar，你可以创建一个带有`@EnableSidecar`注解的Spring Boot程序。我们来看下这个注解都干了什么：

```java
@Enable
@EnableDiscoveryClient
@EnableZuulProxy
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SidecarConfiguration.class)
public @interface EnableSidecar {

}
```
看，hystrix的熔断器、Eureka的服务发现、zuul代理，这些该有的部件，都已经开启。

### 健康检查

接下来需要在application.yml里加入`sidecar.port`和`sidecar.health-uri`的配置。其中`sidecar.port`属性代表这个Node.js应用监听的端口。这是为了让sidecar可以正常的注册到Eureka服务中。`sidecar.health-uri`是一个用来模拟Spring Boot应用健康指标的接口的uri。它必须返回如下形式的json文档：
`health-uri-document`

```json
{
  "status":"UP"
}
```
整个Sidecar应用的`application.yml`如下：
`application.yml`

```yml
server:
  port: 5678
spring:
  application:
    name: sidecar

sidecar:
  port: 8000
  health-uri: http://localhost:8000/health.json
```

### 服务访问

构建完这个应用，你就可以使用`/hosts/{serviceId}`这个API来获取`DiscoveryClient.getInstances()`的结果。这里有一个从`/hosts/customers`返回两个来自不同host的实例信息的例子。如果sidebar运行在5678端口， 那么Node.js应用是可以通过[http://localhost:5678/hosts/{serviceId}](http://localhost:5678/hosts/{serviceId})访问这个api的。

`/hosts/customers`

```json
[
    {
        "host": "myhost",
        "port": 9000,
        "uri": "http://myhost:9000",
        "serviceId": "CUSTOMERS",
        "secure": false
    },
    {
        "host": "myhost2",
        "port": 9000,
        "uri": "http://myhost2:9000",
        "serviceId": "CUSTOMERS",
        "secure": false
    }
]
```

Zuul proxy可以自动每一个注册到Eureka的关联到`/<serviceId>`的服务添加路由，因此customer服务是可以通过`/customers`这个URI访问的。同样假定sidecar监听在5678端口，这样我们的Node.js应用就可以通过[http://localhost:5678/customers](http://localhost:5678/customers)访问这个customer服务。

### Config Server

如果我们使用了Config Server服务，并且把它注册到Eureka，Node.js应用就可以通过Zull Proxy来访问它。如果ConfigServer的serviceId是`configserver`并且Sidecar监听在5678端口，然后就可以通过[http://localhost:5678/configserver](http://localhost:5678/configserver)来访问Config Server。当然这也得益于Eureka，Config Server都提供了基于HTTP协议的Rest接口。

Node.js应用也可以利用Config Server的能力获取一些如YAML格式的配置文档。例如，一个对[http://sidecar.local.spring.io:5678/configserver/default-master.yml](http://sidecar.local.spring.io:5678/configserver/default-master.yml)的访问，可能获得如下的YAML文档的返回：

```yml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  password: password
info:
  description: Spring Cloud Samples
  url: https://github.com/spring-cloud-samples
```
所以将Node.js应用通过Sidecar接入Spring Cloud微服务集群的整体架构，大致就如下图:

![屏幕快照 2016-09-27 下午9.11.17](http://cdn.codedocker.com/2016-09-27-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-27%20%E4%B8%8B%E5%8D%889.11.17.png)


## demo实践

我们假设下有这样一个非常简单的数据，它叫User：

```java
class User {
    private Long id;
    private String username;
    private Integer age;
}
```
看起来非常经典哈！

还有一个数据结构是用来表示书的，Book：

```java
class Book {
    private Long id;
    private Long authorId;
    private String name;
    private String publishDate;
    private String des;
    private String ISBN;
}
```

Book中的authorId对应User的id，现在我们要为这两种数据开发Rest服务了。

首先是User，我们使用spring来开发，先在controller的构造方法里，mock一些假数据users，然后非常简单的一个根据id查用户的Get接口：

```java
@GetMapping("/{id}")
public User findById(@PathVariable Long id) {}
```
启动后，我们curl访问下：
	
	curl localhost:8720/12
	{"id":12,"username":"user12","age":16}

接下来，我们使用Node.js开发Book相关的接口。

由于Node.js社区十分活跃，可选的Rest服务框架非常多。比较主流的有`express`，`koa`, `hapi`等，非常轻量易扩展的也有像`connect`这样的，这里笔者考虑到群众基础和文档丰富度，选择使用[express](https://expressjs.com/)来开发这样一个可以接入Spring Cloud的Rest服务。

```javascript
const express = require('express')
const faker = require('faker/locale/zh_CN')
const logger = require('morgan')
const services = require('./service')

const app = express()

let count = 100
const books = new Array(count)

while (count > 0) {
    books[count] = {
        id: count,
        name: faker.name.title(),
        authorId: parseInt(Math.random() * 100) + 1,
        publishDate: faker.date.past().toLocaleString(),
        des: faker.lorem.paragraph(),
        ISBN: `ISBN 000-0000-00-0`
    }
    count --
}

app.use(logger('combined'))
//服务健康指标接口
app.get('/health', (req, res) => {
    res.json({
        status: 'UP'
    })
})

app.get('/book/:id', (req, res, next) => {
    const id = parseInt(req.params.id)
    if(isNaN(id)){
        next()
    }
    res.json(books[id])
})

//....
```
也是先用[faker](https://github.com/marak/Faker.js/)来mock100条数据,然后写一条简单的get路由。

启动后，我们用浏览器访问下[http://localhost:3000/book/1](http://localhost:3000/book/1)
![屏幕快照 2016-09-28 上午11.56.53](http://cdn.codedocker.com/2016-09-28-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-28%20%E4%B8%8A%E5%8D%8811.56.53.png)

现在我们已经有了两个微服务，接下来我们启动一个Sidecar实例，用于将Node.js接入Spring Cloud。

```java
@SpringBootApplication
@EnableSidecar
public class SidecarApplication {
	public static void main(String[] args) {
		SpringApplication.run(SidecarApplication.class, args);
	}
}
```

非常简单，需要注意的是，在这之前，你需要一个eureka-server，为了测试sidecar代理访问Spring Config的能力，我还使用了config-server，相信熟悉spring cloud的同学应该都知道吧。

在sidecar的配置中，bootstrap.yaml中就是简单指定下服务端口跟config-server的地址,而`node-sidecar.yaml`配置如下：

```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_SERVICE_URL:http://localhost:8700/eureka/}
sidecar:
  port: 3000
  home-page-uri: http://localhost:${sidecar.port}/
  health-uri: http://localhost:${sidecar.port}/health

hystrix:
  command:
    default:
      execution:
        timeout:
          enabled: false
```

这里指定了sidecar所指向的node.js服务的地址,`hystrix.command.default.execution.timeout.enabled: false`主要是因为sidecar使用了hystrix的默认为一秒的超时熔断器，国内访问github的速度你懂的，我在测试时访问config-server经常超时，所以我就把它跟disable掉了，你也可以选择把超时时间配长一点。

将eureka-server，config-server，user-service，node-sidecar, node-book-service都启动后，我们打开eureka的主页面[http://localhost:8700/](http://localhost:8700/)：
![屏幕快照 2016-09-28 下午12.12.34](http://cdn.codedocker.com/2016-09-28-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-28%20%E4%B8%8B%E5%8D%8812.12.34.png)

看到我们的服务都处于UP状态，说明一切正常。接下来在看看Node.js应用的控制台：
![屏幕快照 2016-09-28 下午12.15.00](http://cdn.codedocker.com/2016-09-28-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-28%20%E4%B8%8B%E5%8D%8812.15.00.png)
发现已经有流量打进来了，访问的接口是`/health`，很明显这就是node-sidecar对我们的node应用进行健康检查的调用。

接下来就是见证奇迹的时刻了，我们curl访问sidecar的8741端口:

	curl localhost:8741/user-service/12
	{"id":12,"username":"user12","age":16}
	
跟直接访问user-service结果一致，说明sidecar的Zuul Proxy可以将我们的请求代理到user-service服务。

好了，借助这个代理，我们希望book服务能够提供作者信息的接口：

```javascript
const SIDECAR = {
    uri: 'http://localhost:8741'
}
const USER_SERVICE = 'user-service'
const getUserById = (id) => fetch(`${SIDECAR.uri}/${USER_SERVICE}/${id}`).then((resp)=>resp.json())

app.get('/book/:bookId/author', (req, res, next) => {
    const bookId = parseInt(req.params.bookId)
    if(isNaN(bookId)){
        next()
    }
    const book = books[bookId]
    if(book) {
        let uid = book.authorId
        services.getUserById(uid).then((user) => {
            if(user.id) {
                res.json(user)
            }else{
                throw new Error("user not found")
            }
        }).catch((error)=> next(error))
    }
})

//根据uid，filter出authorId为uid的所有书
app.get('/books', (req, res, next) => {
    const uid = req.query.uid
    res.json(books.filter((book)=>book.authorId == uid))
})
```

我们访问下[http://localhost:3000/book/2/author](http://localhost:3000/book/2/author),可以看到返回了bookId为2的作者信息。但是这里有一个问题，我们并不能像代理到user-service那样通过访问[http://localhost:8741/node-sidecar/book/1](http://localhost:8741/node-sidecar/book/1)来访问Node.js的接口，那么怎么让user-service拿到book-service的数据呢？看下最开始的理论知识部分，我们可以通过访问`/hosts/<serviceId>`获取到各个服务的相关信息，我们来试下访问[http://localhost:8741/hosts/node-sidecar](http://localhost:8741/hosts/node-sidecar)得到如下结果：
![屏幕快照 2016-09-28 下午2.30.23](http://cdn.codedocker.com/2016-09-28-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-28%20%E4%B8%8B%E5%8D%882.30.23.png)

可以看到返回信息里有Node.js应用的uri等信息，那么是不是我们可以先访问下sidecar的这个接口，拿到真实的uri之后，再来调用book-service的`/books?uid=<uid>`接口呢？当然可以，事实上spring cloud中已经有工具帮我们做这个事情，就是`Feign`，新建`BookFeighClient.java`：

```java
@FeignClient(name = "node-sidecar")
public interface BookFeignClient {
    @RequestMapping("/books")
    public List<Book> findByUid(@RequestParam("uid") Long id);
}
```
`FeignClient`可以自动根据serviceId去Eureka上找到对应的服务地址，如果该服务的实例不止一个，就会使用Ribbon进行客户端负载均衡，再加上一些像`RequestMapping`的注解，可以让客户端跟服务端controller保持一致。通过定义的这个`findByUid`方法，我们就可以轻松调用上面Node.js中定义的`/books?uid=<uid>`的接口了。这一点儿，也和我们上面画的sidecar架构图一致。

有了，我们再在user-service中定义这样一个新类型Author，它继承自User，加了一个books字段:

```java
class Author extends User {
    private List<Book> books;
}
```

再加入一个获取author的接口：

```java
@GetMapping("/author/{id}")
public Author getAuthor(@PathVariable Long id) {
   List<Book> books = bookFeignClient.findByUid(id);
   User user = findById(id);
   Author author = new Author();
   author.setId(user.getId());
   author.setUsername(user.getUsername());
   author.setAge(user.getAge());
   author.setBooks(books);
   return author;
}
```
逻辑也很简单，获取对应user，根据uid从bookFeignClient获取books，然后构建author返回。

我们访问下[http://localhost:8720/author/11](http://localhost:8720/author/11)看下返回结果：
![屏幕快照 2016-09-28 下午3.58.12](http://cdn.codedocker.com/2016-09-28-%E5%B1%8F%E5%B9%95%E5%BF%AB%E7%85%A7%202016-09-28%20%E4%B8%8B%E5%8D%883.58.12.png)
需要注意由于是随机数据，可能需要换几个authorId才能看到这样的结果。

好了，到现在为止，我们已经完成了JAVA和Node.js两种语言借助sidecar和通用的http协议完成互相调用的全过程。关于更多的类似从config-server获取配置信息，从Eureka获取应用信息等操作，可以去下载我实验用的源码来了解。

我把整个DEMO放在了我的github中了，大家可以直接clone下来

	git clone https://github.com/marshalYuan/spring-cloud-example.git
	
整个工程大致是这样的：

* eureka-server //对应上图的Eureka Server
* config-server //对应上图的Config Server
* config-repo //config-server仓库地址的searchPath
* user-service //java开发的服务，既是服务提供者(Provider)也是消费者(Coustomer)
* node-sidecar //一个sidecar实例，负责连接node和spring-cloud
* book-service-by-node //express.js开发的Rest服务

大家可以按照：
	
	eureka-server -> config-server -> user-service -> book-service-by-node -> node-sidecar
	
这样的顺序启动这五个应用，由于是测试用demo，所以有bug我也不管哈。

## 写在结尾

正如开篇所说，得益于通用的Http协议和Netflix丰富的套件，我们可以将很多像Node.js，PHP，Python这样的非JVM语言接入Spring Cloud这个非常成熟的微服务框架，来迅速构建我们的微服务业务系统。你可能会说为什么不都用java呢？确实，一个系统单一语言开发维护成本确实会低很多，但还有一些其他情况值得我们去选择sidecar方案。

比如，历史包袱太重，想切到java平台，但是又不想完全重写过去的服务，这样就可以以统一协议为代价来进行整合，从java切到其他平台亦是如此。

还有一个说法叫"拥抱语言红利"，选择一种开发语言就代表选择一种编程思想已经这门开发语言对应的工具和库。比如现在很流行用Python做数据分析，那么微服务系统中这一部分的业务是不是可以用Python开发啊；Node.js的异步事件事件驱动机制很优秀，能不能用它来开发一些需要处理大量异步请求的服务啊；诸如此类。这里确实不是在引发"最优语言圣战"哈，私以为脱离了使用场景和生态来进行语言优劣的比较就是耍流氓。就拿计算100以内的所有[勾股数](http://baike.baidu.com/view/148142.htm)为例，我不觉得有什么语言能像Haskell的代码这样简洁易懂：

```
[ (x,y,z) | x <- [1..100], y <- [x..100], z <- [x..100], x*x + y*y == z*z ]
```
再说了，我们标题中选的是Node.js，最好的语言明明是PHP啊！逃~~~

