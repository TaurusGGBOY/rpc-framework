package github.ggb.remote.transport.netty.codec;

import github.ggb.compress.Compress;
import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.extension.ExtensionLoader;
import github.ggb.remote.Constants.RpcConstants;
import github.ggb.remote.dto.RpcMessage;
import github.ggb.remote.dto.RpcRequest;
import github.ggb.remote.dto.RpcResponse;
import github.ggb.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

// 0123 魔数； 4版本； 5678长度； 9类型； 10编码类型； 11压缩类型； 1213141516 请求id
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder(){
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (!(decoded instanceof ByteBuf)){
            return decoded;
        }
        ByteBuf frame = (ByteBuf) decoded;
        if (frame.readableBytes() < RpcConstants.TOTAL_LENGTH) {
            return decoded;
        }
        try {
            return decodeFrame(frame);
        } catch (Exception e) {
            log.error("Decode frame error!", e);
            throw e;
        }finally {
            frame.release();
        }
    }

    private Object decodeFrame(ByteBuf in) {
        checkMagicNumber(in);
        checkVersion(in);
        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
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
        if ( bodyLength < 0) {
            return rpcMessage;
        }
        byte[] bs = new byte[bodyLength];
        in.readBytes(bs);
        String compressName = CompressTypeEnum.getName(compressType);
        Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
        bs = compress.decompress(bs);
        String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
        log.info("codec name:[{}]", codecName);
        Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
        if (messageType == RpcConstants.REQUEST_TYPE) {
            RpcRequest tmpValue = serializer.deserialize(bs, RpcRequest.class);
            rpcMessage.setData(tmpValue);
        }else{
            RpcResponse tmpValue = serializer.deserialize(bs, RpcResponse.class);
            rpcMessage.setData(tmpValue);
        }
        return rpcMessage;
    }

    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version is not compatible " + version);
        }
    }

    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]){
                throw new IllegalArgumentException("unknow magic code:" + Arrays.toString(tmp));
            }
        }
    }

}