package github.ggb.remote;

import github.ggb.dto.RpcRequest;
import github.ggb.extension.SPI;

@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest);
}
