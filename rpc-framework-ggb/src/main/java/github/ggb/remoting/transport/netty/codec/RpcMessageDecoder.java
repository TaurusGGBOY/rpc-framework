package github.ggb.remoting.transport.netty.codec;

import github.ggb.compress.Compress;
import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.extension.ExtensionLoader;
import github.ggb.remoting.Constants.RpcConstants;
import github.ggb.remoting.dto.RpcMessage;
import github.ggb.remoting.dto.RpcRequest;
import github.ggb.remoting.dto.RpcResponse;
import github.ggb.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

// 0123 魔数； 4版本； 5678长度； 9类型； 10编码类型； 11压缩类型； 1213141516 请求id
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (!(decoded instanceof ByteBuf)) {
            return decoded;
        }
        ByteBuf frame = (ByteBuf) decoded;
        // 如果读的帧小 就直接decode
        if (frame.readableBytes() < RpcConstants.TOTAL_LENGTH) {
            return decoded;
        }
        // 如果读的帧大 就调另一个
        try {
            return decodeFrame(frame);
        } catch (Exception e) {
            log.error("Decode frame error!", e);
            throw e;
        } finally {
            // bytebuf要release的
            frame.release();
        }
    }

    // 反正最后也还是要拼接成一个大的
    private Object decodeFrame(ByteBuf in) {
        checkMagicNumber(in);
        checkVersion(in);
        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        // 这还是在洗消息
        RpcMessage rpcMessage = RpcMessage.builder()
                .codec(codecType)
                .requestId(requestId)
                .messageType(messageType).build();
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }

        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength < 0) {
            return rpcMessage;
        }
        byte[] bs = new byte[bodyLength];
        in.readBytes(bs);
        // 为什么要动态的加载？因为这里可能随便指定 可以在不入侵代码的情况下 增加或删除 不同的压缩和序列化
        String compressName = CompressTypeEnum.getName(compressType);
        Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
        bs = compress.decompress(bs);
        String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
        log.info("codec name:[{}]", codecName);
        Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
        // 前面已经处理了心跳了
        if (messageType == RpcConstants.REQUEST_TYPE) {
            RpcRequest tmpValue = serializer.deserialize(bs, RpcRequest.class);
            rpcMessage.setData(tmpValue);
        } else {
            RpcResponse tmpValue = serializer.deserialize(bs, RpcResponse.class);
            rpcMessage.setData(tmpValue);
        }
        return rpcMessage;
    }

    // 检查版本
    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version is not compatible " + version);
        }
    }

    // 读4位检查魔数
    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("unknow magic code:" + Arrays.toString(tmp));
            }
        }
    }

}