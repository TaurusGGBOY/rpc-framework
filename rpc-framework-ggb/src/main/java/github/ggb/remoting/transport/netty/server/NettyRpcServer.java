package github.ggb.remoting.transport.netty.server;

import github.ggb.config.CustomShutdownHook;
import github.ggb.config.RpcServiceConfig;
import github.ggb.factory.SingletonFactory;
import github.ggb.provider.Impl.ZkServiceProviderImpl;
import github.ggb.provider.ServiceProvider;
import github.ggb.remoting.transport.netty.codec.RpcMessageDecoder;
import github.ggb.remoting.transport.netty.codec.RpcMessageEncoder;
import github.ggb.utils.RuntimeUtil;
import github.ggb.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
// 所以核心主要是三个 监听队列 读写队列 处理队列
public class NettyRpcServer {
    // 这个端口是用来监听的
    public static final int PORT = 9998;
    // 统一线程安全工厂方法生成服务提供者
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        // 在zk上注册
        serviceProvider.publishService(rpcServiceConfig);
    }

    // catch之后Lombok.sneakyThrow(e) 不用手动处理
    @SneakyThrows
    public void start() {
        // 相当于守护线程 关闭之前取消所有的注册
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        // 这里需要添加netty的maven依赖
        // boss一个线程nio监听就完事了
        // 开了一个线程 监听
        // 一个group 包含多个eventloop
        // 一个eventloop是一个线程 保证线程安全
        // 监听可以开多个线程 监听到了之后 会传给worker的选择器 选择器会选择一个事件循环跑读写
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 默认worker是后面会改成2*cpu数量
        // 负责处理读写
        EventLoopGroup workGroup = new NioEventLoopGroup();
        // 线程数就是cpu数 加一个工厂
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(RuntimeUtil.cpus(), ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));
        try {
            // 启动辅助类
            ServerBootstrap b = new ServerBootstrap();
            // 单线程模型就是两个都绑定在自己
            b.group(bossGroup, workGroup)
                    // channel 是什么？
                    // 操作的抽象类
                    .channel(NioServerSocketChannel.class)
                    // 可以一直往child里面加属性
                    // 不开启nagle防止消息阻塞
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 心跳
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 全连接+半连接队列的长度
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 默认参数日志为INFO
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 这个强制类型转换也迷
                    // 对于每一个boss 和 worker线程都执行
                    // 每一个会有一个自己的socketchannel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // encode和decode
                            // 数据/事件
                            // 混合的时候可以通过接口区分
                            // 心跳 定时任务 读 写 读写 30s没读就断连接
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new RpcMessageEncoder())
                                    .addLast(new RpcMessageDecoder())
                                    // 监听channel？
                                    // 多线程执行队列
                                    // 每个消息调用一手handler
                                    .addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定  这里本来是个异步 但是这里sync了
            // 这里是要await的？
            // 开始事件循环了吗？ 没有开启事件循环
            ChannelFuture f = b.bind(host, PORT).sync();
            // f.channel是获取channel的
            // closeFuture
            // 懂了…… 监听channel的close消息
            // 在接收到close之前一直阻塞
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("start server error", e);
        }finally {
            log.error("shutdown boss and worker");
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }


}