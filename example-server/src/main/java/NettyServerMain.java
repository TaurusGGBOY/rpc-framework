import github.ggb.HelloService;
import github.ggb.HelloServiceImpl;
import github.ggb.HelloServiceImpl2;
import github.ggb.annotation.RpcScan;
import github.ggb.config.RpcServiceConfig;
import github.ggb.remoting.transport.netty.server.NettyRpcServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RpcScan(basePackage = {"github.ggb"})
public class NettyServerMain {
    public static void main(String[] args) {
//        hello1();
        hello2();
    }

    static void hello1(){
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        HelloService helloServiceImpl2 = new HelloServiceImpl2();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder().group("test2").version("version2").service(helloServiceImpl2).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        nettyRpcServer.start();
    }

    static void hello2(){
        HelloService helloService = new HelloServiceImpl();
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        rpcServiceConfig.setService(helloService);
        nettyRpcServer.start();
    }
}