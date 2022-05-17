package github.ggb.annotation;

import java.lang.annotation.*;

//  TODO 如果不加注解会怎么样
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RpcService {

    String version() default "";

    String group() default "";
}
