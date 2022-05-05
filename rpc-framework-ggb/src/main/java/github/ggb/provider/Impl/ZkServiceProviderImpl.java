package github.ggb.provider.Impl;

import github.ggb.config.RpcServiceConfig;
import github.ggb.enums.RpcErrorMessageEnum;
import github.ggb.exception.RpcException;
import github.ggb.extension.ExtensionLoader;
import github.ggb.provider.ServiceProvider;
import github.ggb.registry.ServiceRegistry;
import github.ggb.remote.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    // 服务就是一个object？
    private final Map<String, Object> serviceMap;
    // 保存已经注册的服务
    private final Set<String> registeredService;
    //服务注册机构
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        // 用动态加载加载zk获取一个实例
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    // 添加服务俩 感觉可以缩减成一个啊？serviceMap.keySet()不就是registeredService
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
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
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}