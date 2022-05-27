package github.ggb.extension;

import java.lang.annotation.*;

// service provider interface
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
}
