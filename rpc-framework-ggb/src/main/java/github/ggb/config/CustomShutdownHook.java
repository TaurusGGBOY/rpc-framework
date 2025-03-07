package github.ggb.config;

import github.ggb.registry.zk.util.CuratorUtils;
import github.ggb.remoting.transport.netty.server.NettyRpcServer;
import github.ggb.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
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
        // 关闭的时候清除zk注册
        // 这样确实本地的就是zk上的
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
            } catch (UnknownHostException ignored) {

            }
            ThreadPoolFactoryUtil.shutDownAllThreadPool();
        }));
    }
}