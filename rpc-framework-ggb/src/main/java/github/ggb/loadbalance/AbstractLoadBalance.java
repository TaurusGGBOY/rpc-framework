package github.ggb.loadbalance;

import github.ggb.remoting.dto.RpcRequest;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    // 给定全集 看按照什么策略选择
    // 那么LB方得存一些元信息保证负载啊
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if (serviceAddresses == null || serviceAddresses.isEmpty()) {
            return null;
        }
        if (1 == serviceAddresses.size()) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}