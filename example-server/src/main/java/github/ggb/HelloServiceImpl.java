package github.ggb;

import github.ggb.Hello;
import github.ggb.HelloService;
import github.ggb.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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