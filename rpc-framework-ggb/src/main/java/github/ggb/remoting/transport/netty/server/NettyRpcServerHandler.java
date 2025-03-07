package github.ggb.remoting.transport.netty.server;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.RpcResponseCodeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.factory.SingletonFactory;
import github.ggb.remoting.Constants.RpcConstants;
import github.ggb.remoting.handler.RpcRequestHandler;
import github.ggb.remoting.dto.RpcMessage;
import github.ggb.remoting.dto.RpcRequest;
import github.ggb.remoting.dto.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 核心就是这个函数 如何处理Netty过来的消息
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;
    // 没有上下文 所以直接单例就行
    public NettyRpcServerHandler() {
        // 这是个线程安全的单例
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    // 事件来了 如果是读空闲事件 就要关闭链接咯！
    // 主要就是读写事件 还有第一个事件等特殊事件
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (IdleState.READER_IDLE == state) {
                log.info("Idle check happen, so close the connection");
                ctx.close();
            }else{
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    @Override
    // 读事件来了 开读
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (!(msg instanceof RpcMessage)) {
                return;
            }
            log.info("server receive msg: [{}] ", msg);
            byte messageType = ((RpcMessage) msg).getMessageType();
            RpcMessage rpcMessage = new RpcMessage();
            // 编码解码用HESSIAN
            rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
            // 压缩用GZIP
            rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
            // 特判心跳 这个心跳和netty级别的心跳不一样 这个是服务级别的心跳
            // 如果是ping 返回pong
            if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                rpcMessage.setData(RpcConstants.PONG);
            }else{
                // 如果是pong 说明服务还在可用状态
                RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                // 重点就是这个handle了
                Object result = rpcRequestHandler.handle(rpcRequest);
                log.info(String.format("server get result %s", result.toString()));
                rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                // 如果channel是活跃并且可写
                if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                    RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                    rpcMessage.setData(rpcResponse);
                }else{
                    // 这个地方还是有问题 还是要带上requestId 这样check才能过 不是直接throw而是retry
                    RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                    rpcMessage.setData(rpcResponse);
                    log.error("not writeable now, message dropped");
                }
            }
            // 写并且刷新rpcMesssage
            ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }finally {
            // 从inbound中读取的bytebuf需要手动释放 用的引用计数
            ReferenceCountUtil.release(msg);
        }
    }
}