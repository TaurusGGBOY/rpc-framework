package github.ggb;

import github.ggb.Hello;
import github.ggb.HelloService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloServiceImpl2 implements HelloService {

    static{
        System.out.println("HelloServiceImpl2被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("hello service impl2 收到{}", hello.getMessage());
        String result = "hello description is " + hello.getDescription();
        log.info("hello service impl2 返回 {}", result);
        return result;
    }

}
