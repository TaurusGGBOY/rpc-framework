package github.ggb.annotation;

import java.lang.annotation.*;

// 如果不加注解会怎么样
// 感觉注释这个无所谓了
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
// 子类会继承该注解
@Inherited
public @interface RpcService {

    String version() default "";

    String group() default "";
}
