package github.ggb.registry;

import github.ggb.dto.RpcRequest;
import github.ggb.extension.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDiscovery {
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
