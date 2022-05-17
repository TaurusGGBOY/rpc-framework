package github.ggb.spring;

import github.ggb.annotation.RpcReference;
import github.ggb.annotation.RpcService;
import github.ggb.config.RpcServiceConfig;
import github.ggb.extension.ExtensionLoader;
import github.ggb.factory.SingletonFactory;
import github.ggb.provider.Impl.ZkServiceProviderImpl;
import github.ggb.provider.ServiceProvider;
import github.ggb.proxy.RpcClientProxy;
import github.ggb.remote.RpcRequestTransport;
import github.ggb.remote.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

@Slf4j
public class SpringBeanPostProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor(ServiceProvider serviceProvider, RpcRequestTransport rpcClient) {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension("netty");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!bean.getClass().isAnnotationPresent(RpcService.class)) {
            return bean;
        }
        log.info("[{}] is annotated with [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group(rpcService.group())
                .version(rpcService.version())
                .service(bean)
                .build();
        serviceProvider.publishService(rpcServiceConfig);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (null == rpcReference){
                continue;
            }
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcReference.group())
                    .version(rpcReference.version())
                    .build();
            RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
            Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
            declaredField.setAccessible(true);
            try {
                declaredField.set(bean, clientProxy);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return bean;
    }
}