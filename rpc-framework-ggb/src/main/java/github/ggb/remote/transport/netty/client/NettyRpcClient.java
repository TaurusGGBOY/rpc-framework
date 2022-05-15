package github.ggb.remote.transport.netty.client;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.extension.ExtensionLoader;
import github.ggb.factory.SingletonFactory;
import github.ggb.registry.ServiceDiscovery;
import github.ggb.remote.Constants.RpcConstants;
import github.ggb.remote.RpcRequestTransport;
import github.ggb.remote.dto.RpcMessage;
import github.ggb.remote.dto.RpcRequest;
import github.ggb.remote.dto.RpcResponse;
import github.ggb.remote.transport.netty.codec.RpcMessageDecoder;
import github.ggb.remote.transport.netty.codec.RpcMessageEncoder;
import github.ggb.remote.transport.netty.server.NettyRpcServerHandler;
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
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                                .addLast(new RpcMessageEncoder())
                                .addLast(new RpcMessageDecoder())
                                .addLast(new NettyRpcServerHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    // TODO 这个
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
        return completableFuture.get();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        Channel channel = getChannel(inetSocketAddress);
        if (!channel.isActive()) {
            throw new IllegalStateException();
        }
        unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
        RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(RpcConstants.REQUEST_TYPE).build();
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