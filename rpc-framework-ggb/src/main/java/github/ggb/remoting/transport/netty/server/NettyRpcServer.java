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
public class NettyRpcServer {
    // 这个端口是用来监听的
    public static final int PORT = 9998;
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    // TODO 这个注解是干啥的
    @SneakyThrows
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        // 这里需要添加netty的maven依赖
        // boss一个线程nio监听就完事了
        // TODO worker无限？
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();
        // 线程数就是cpu数 加一个工厂
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(RuntimeUtil.cpus(), ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    // TODO channel 是什么？
                    .channel(NioServerSocketChannel.class)
                    // TODO 这个是啥？
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 心跳
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 全连接队列？
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // TODO 为什么是child
                    // 这个强制类型转换也迷
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // TODO 这个是啥来着……
                            // encode和decode
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new RpcMessageEncoder())
                                    .addLast(new RpcMessageDecoder())
                                    // 监听channel？
                                    .addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定  这里本来是个异步 但是这里sync了
            // 这里是要await的？
            // 开始事件循环了吗？ 没有开启事件循环
            ChannelFuture f = b.bind(host, PORT).sync();
            // TODO 关闭？
            // f.channel是获取channel的
            // closeFuture
            // 懂了…… 监听channel的close消息
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