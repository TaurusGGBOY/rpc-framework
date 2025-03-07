package github.ggb.annotation;

import java.lang.annotation.*;

// JAVA元注解
// 是否应当被包含在 javadoc
// 生命周期
// 作用目标 修饰什么的
// 是否允许继承
@Documented
@Retention(RetentionPolicy.RUNTIME)
// 比如就用来修饰了声明的RpcService rpcService 指定查找
// 查找的对象是 RpcService这种
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    String version() default "";

    String group() default "";
}
