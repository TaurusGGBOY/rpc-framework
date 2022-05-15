package github.ggb.remote.transport.netty.server;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.RpcResponseCodeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.factory.SingletonFactory;
import github.ggb.remote.Constants.RpcConstants;
import github.ggb.remote.RpcRequestHandler;
import github.ggb.remote.dto.RpcMessage;
import github.ggb.remote.dto.RpcRequest;
import github.ggb.remote.dto.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            // TODO 为啥空闲了就可以close了
            if (IdleState.READER_IDLE == state) {
                log.info("Idle check happen, so close the connection");
                ctx.close();
            }else{
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    @Override
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
            // 如果是ping 返回pong
            if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                rpcMessage.setData(RpcConstants.PONG);
            }else{
                // 否则就要handler去处理了
                RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                Object result = rpcRequestHandler.handle(rpcRequest);
                log.info(String.format("server get result %s", result.toString()));
                rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                // 如果channel是活跃并且可写
                if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                    RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                    rpcMessage.setData(rpcResponse);
                }else{
                    // TODO 这个有问题rpc接收方也收不到啊？谁来处理失败？
                    RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                    rpcMessage.setData(rpcResponse);
                    log.error("not writeable now, message dropped");
                }
            }
            // 写并且刷新rpcMesssage
            ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }finally {
            // TODO 这个是干什么的
            ReferenceCountUtil.release(msg);
        }
    }
}