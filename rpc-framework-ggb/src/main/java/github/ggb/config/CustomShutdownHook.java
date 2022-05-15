package github.ggb.config;

import github.ggb.remote.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Slf4j
public class CustomShutdownHook {
    // 这个单例才对嘛
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook(){ return CUSTOM_SHUTDOWN_HOOK; }

    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        // TODO 这是啥哦……
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.clear();
            } catch (UnknownHostException ignored) {

            }
            ThreadPoolFactoryUtil.shutdownAllThreadPool();
        }));
    }
}