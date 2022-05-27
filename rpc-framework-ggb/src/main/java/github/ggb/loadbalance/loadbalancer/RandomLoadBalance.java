package github.ggb.loadbalance.loadbalancer;

import github.ggb.loadbalance.AbstractLoadBalance;
import github.ggb.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    // 随机选择很简单 就只用随机出一个数 取出来就行了
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}