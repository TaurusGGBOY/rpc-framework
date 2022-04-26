# 从零开始手把手实现RPC框架

## 文档

+ 为什么要RPC：服务提供的方法不在同一个内存空间，需要网络编程
+ RPC作用：调用远程方法像调用本地方法一样简单
+ 角色：客户端，客户端代理，网络传输，服务端拦截器，服务端
+ 常见RPC框架：Dubbo（Java），Motan（新浪），gPRC（ProtoBuf，跨语言，没有服务治理），Thrift（语言支持更多，没有服务治理）
+ 角色：服务器端，客户端，注册端
+ 注册中心：zk，Nacos，Redis
+ zk：高可用，高性能，稳定，发布订阅，负载均衡，命名，分布式协调通知，集群管理，master选举，分布式锁，分布式队列
+ 序列化：网络传输，文件存储，对象存储到数据库
+ 序列化选项：自带，Kryo（针对java，变长存储，较高运行速度，较小字节码体积），protobuf（需要自定义序列化的对象，tidb用的），protostuff（改进了pb），hession（dubbo默认）
+ Netty特点：统一API，阻塞非阻塞，线程模型，TCP粘包拆包，协议栈，支持无连接（？），吞吐量，延迟，资源，内存复制，SSL支持，社区活跃，Dubbo，RocketMQ，ES，gRPC都用了
+ JDK代理：只能代理实现了接口的类，实现InvocationHandler，保存代理对象object，实现invoke方法，使用代理对象的工厂类生成代理对象 JDK代理更快
+ CGLIB动态代理：继承Callback 实现intercept，getPxoxy中Enhancer 设置类加载器，被代理类，方法拦截器，create()创建
+ CGLIB：生成一个被代理类的子类 不能代理生命为final的 如String
+ 动态代理：不需要手动实现接口 不需要增加方法就重写 JVM运行时动态生成的字节码

## 写的流程
  + 从上往下
  + RpcRequestTransport
  + RpcMessage RpcRequest RpcResponse
  + ServiceProvider ZkServiceProviderImpl
  + RpcServiceConfigd等

## 知识点
  + 如何注册一个注解
    + 在同一个package下创建`public @interface SPI`
  + maven项目在调用其他module包的时候要在当前module的pom.xml下添加dependency

## 遇到的问题

+ @SPI注解没有生效
  + 需要自己去定义这个注解 放在common库里
+ 注解的作用
  + 告诉编译器某些事情
  + 比如@override 不运行
+ @Target(ElementType.TYPE)
    + 说明是一个type 可以修饰 类 接口 或者 枚举？
+ @Decumented作用
    + 生成javadoc的时候会显示
+ @Retention(RetentionPolicy.RUNTIME)
    + 生命周期：注解在保存到class文件中，jvm加载class文件之后，仍然存在
    + 作用：生命周期为运行时
+ dto啥意思
+ @Builder注解是干啥的
  + 生成了一个builder类 里面有个本对象 里面有各个方法的赋值方法
  + people.builder().name("aaa").id("bbb") 然后可以流式执行
+ @ToString注解输出是什么
+ 实现Serializable要做什么 
+ enum啥情况为啥能在里面定义成员变量的
+ @Slf4j有什么用
+ @Component有什么用
+ getCanonicalName
+ class.cast
+ computeIfAbsent
+ getDeclaredConstructor
+ InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException 为什么是这几个错
+ publish和add啥区别？
