package github.ggb.loadbalance;

import github.ggb.extension.SPI;
import github.ggb.remote.dto.RpcRequest;

import java.util.List;

@SPI
public interface LoadBalance {

    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
