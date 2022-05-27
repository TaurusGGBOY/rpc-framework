package github.ggb.remoting.handler;

import github.ggb.exception.RpcException;
import github.ggb.factory.SingletonFactory;
import github.ggb.provider.Impl.ZkServiceProviderImpl;
import github.ggb.provider.ServiceProvider;
import github.ggb.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;
    public RpcRequestHandler(){
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public Object handle(RpcRequest rpcRequest) {
        // 首先从请求中获取service的名字
        // 这个地方反反复复都是本地取？
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        // 然后调用请求 并返回结果
        return invokeTargetMethod(rpcRequest, service);
    }

    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            // 反射通过方法加参数 获得唯一方法
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}