package github.ggb.remoting.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcMessage{

    private byte messageType;
    // TODO 这俩是啥
    private byte codec;
    private byte compress;
    private int requestId;
    private Object data;

}