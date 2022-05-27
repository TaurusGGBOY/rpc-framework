package github.ggb.remoting.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcMessage{

    private byte messageType;
    // 编码器
    private byte codec;
    // 压缩
    private byte compress;
    private int requestId;
    private Object data;

}