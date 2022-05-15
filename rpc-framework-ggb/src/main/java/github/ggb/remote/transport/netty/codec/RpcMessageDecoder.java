package github.ggb.remote.transport.netty.codec;

import github.ggb.enums.CompressTypeEnum;
import github.ggb.enums.SerializationTypeEnum;
import github.ggb.extension.ExtensionLoader;
import github.ggb.remote.Constants.RpcConstants;
import github.ggb.remote.dto.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
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
    }
}