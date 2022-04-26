package github.ggb.remote.server;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import github.ggb.config.RpcServiceConfig;
import github.ggb.factory.SingletonFactory;
import github.ggb.provider.Impl.ZkServiceProviderImpl;
import github.ggb.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyRpcServer {
    public static final int PORT = 9998;
    private final ServiceProvider serverProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerServcice(RpcServiceConfig rpcServiceConfig){
        serverProvider.publishService(rpcServiceConfig);}

}