package github.ggb.remoting.transport;

import github.ggb.extension.SPI;
import github.ggb.remoting.dto.RpcRequest;

@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest);
}
