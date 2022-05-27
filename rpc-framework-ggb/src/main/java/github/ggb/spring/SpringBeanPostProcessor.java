package github.ggb.spring;

import github.ggb.annotation.RpcReference;
import github.ggb.annotation.RpcService;
import github.ggb.config.RpcServiceConfig;
import github.ggb.extension.ExtensionLoader;
import github.ggb.factory.SingletonFactory;
import github.ggb.provider.Impl.ZkServiceProviderImpl;
import github.ggb.provider.ServiceProvider;
import github.ggb.proxy.RpcClientProxy;
import github.ggb.remoting.transport.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

// 可以借助springbean的特性 在初始化之前公布服务
// 初始化之后
@Slf4j
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension("netty");
    }

    @SneakyThrows
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
        // 对每个字段都要设置个代理……有点麻烦 自定义bean
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (null == rpcReference){
                continue;
            }
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcReference.group())
                    .version(rpcReference.version())
                    .build();
            // 哦 这里要实现spring的单例+代理
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