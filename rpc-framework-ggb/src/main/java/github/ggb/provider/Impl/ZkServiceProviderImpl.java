package github.ggb.provider.Impl;

import github.ggb.config.RpcServiceConfig;
import github.ggb.enums.RpcErrorMessageEnum;
import github.ggb.exception.RpcException;
import github.ggb.extension.ExtensionLoader;
import github.ggb.provider.ServiceProvider;
import github.ggb.registry.ServiceRegistry;
import github.ggb.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    // 服务就是一个object？
    private final Map<String, Object> serviceMap;
    //服务注册机构
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        // 用动态加载加载zk获取一个实例
//        这个动态加载有什么说法？ 一定要动态加载吗
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    // 添加服务俩 感觉可以缩减成一个啊？serviceMap.keySet()不就是registeredService
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (serviceMap.containsKey(rpcServiceName)) {
            return;
        }
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("add service:{} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    // 为什么这里不用管线程安全问题……
    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    // 添加到map里 然后再注册机构注册
    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
            // 本地修改
            this.addService(rpcServiceConfig);
            // 在zk上注册服务
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}