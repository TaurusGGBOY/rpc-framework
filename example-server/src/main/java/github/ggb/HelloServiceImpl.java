package github.ggb;

import github.ggb.Hello;
import github.ggb.HelloService;
import github.ggb.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 运行到这个地方的时候会自动注册一个Service 并且带有下面这些属性
// 是NettyServerMain里面手动注册的 如果没有调用的话 也还是不会注册
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    static{
        System.out.println("HelloServiceImpl被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("hello service impl 收到{}", hello.getMessage());
        String result = "hello description is " + hello.getDescription();
        log.info("hello service impl 返回 {}", result);
        return result;
    }
}