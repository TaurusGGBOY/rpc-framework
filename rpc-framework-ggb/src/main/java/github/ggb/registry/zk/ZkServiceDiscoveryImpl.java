package github.ggb.registry.zk;

import github.ggb.enums.RpcErrorMessageEnum;
import github.ggb.exception.RpcException;
import github.ggb.extension.ExtensionLoader;
import github.ggb.loadbalance.LoadBalance;
import github.ggb.registry.ServiceDiscovery;
import github.ggb.registry.zk.util.CuratorUtils;
import github.ggb.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        // 这个就是 动态加载？
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 获取client
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 这个地方获取该服务所有的儿子们
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (serviceUrlList.isEmpty()) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 可能一个服务有很多备选 公平选择保证复杂均衡
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        // 查找服务最后直接给我地址和端口就行
        return new InetSocketAddress(host, port);
    }
}