package github.ggb.remote.transport.netty;

import github.ggb.extension.SPI;
import github.ggb.remote.dto.RpcRequest;

@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest);
}
