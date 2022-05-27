package github.ggb.remoting.transport.netty.client;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.extension.ExtensionLoader;
import github.ggb.factory.SingletonFactory;
import github.ggb.registry.ServiceDiscovery;
import github.ggb.remoting.Constants.RpcConstants;
import github.ggb.remoting.dto.RpcMessage;
import github.ggb.remoting.dto.RpcRequest;
import github.ggb.remoting.dto.RpcResponse;
import github.ggb.remoting.transport.RpcRequestTransport;
import github.ggb.remoting.transport.netty.codec.RpcMessageDecoder;
import github.ggb.remoting.transport.netty.codec.RpcMessageEncoder;
import github.ggb.remoting.transport.netty.server.NettyRpcServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class NettyRpcClient implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        eventLoopGroup = new NioEventLoopGroup();
        // nio 用
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 5s断线就丢掉
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                                .addLast(new RpcMessageEncoder())
                                .addLast(new RpcMessageDecoder())
                                .addLast(new NettyRpcClientHandler());
                    }
                });
        // 服务发现是zk
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        // 一个inetAddress对应一个channel
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                throw new IllegalStateException();
            }
            log.info("the client has connected [{}] successful!", inetSocketAddress.toString());
            completableFuture.complete(future.channel());
        });
        // 成功在这
        // 但是必须这么写吗……
        return completableFuture.get();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 先在zk上查了再看发不发
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        Channel channel = getChannel(inetSocketAddress);
        if (!channel.isActive()) {
            throw new IllegalStateException();
        }
        // 可以的 单例其他地方会用到
        unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
        RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(RpcConstants.REQUEST_TYPE).build();
        // 这里不阻塞
        channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("client and message:[{}]", rpcMessage);
            } else {
                future.channel().close();
                resultFuture.completeExceptionally(future.cause());
                log.error("send failed:", future.cause());
            }
        });
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (null != channel) {
            return channel;
        }
        channel = doConnect(inetSocketAddress);
        channelProvider.set(inetSocketAddress, channel);
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}