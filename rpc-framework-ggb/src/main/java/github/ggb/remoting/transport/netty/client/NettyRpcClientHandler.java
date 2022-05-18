package github.ggb.remoting.transport.netty.client;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.factory.SingletonFactory;
import github.ggb.remoting.Constants.RpcConstants;
import github.ggb.remoting.dto.RpcMessage;
import github.ggb.remoting.dto.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {
    private final UnprocessedRequests unprocessedRequests;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    // TODO
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        IdleState state = ((IdleStateEvent) evt).state();
        if (state != IdleState.WRITER_IDLE) {
            return;
        }
        log.info("write idle happen [{}]", ctx.channel().remoteAddress());
        Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setCodec(SerializationTypeEnum.PROTOSTUFF.getCode());
        rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
        rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
        rpcMessage.setData(RpcConstants.PING);
        channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    // TODO
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("client catch exception:", cause);
        cause.printStackTrace();
        ctx.close();
    }

    // TODO
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       try {
           log.info("client receive msg: [{}]", msg);
           if (!(msg instanceof RpcMessage)) {
               return;
           }
           RpcMessage tmp = (RpcMessage) msg;
           byte messageType = tmp.getMessageType();
           if (RpcConstants.HEARTBEAT_RESPONSE_TYPE == messageType) {
               log.info("heart [{}]", tmp.getData());
           } else if (RpcConstants.RESPONSE_TYPE == messageType) {
               RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
               unprocessedRequests.complete(rpcResponse);
           }
       }finally {
           ReferenceCountUtil.release(msg);
       }
    }
}