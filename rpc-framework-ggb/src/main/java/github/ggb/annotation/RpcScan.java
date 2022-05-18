package github.ggb.annotation;

import github.ggb.spring.CustomScannerRegister;
import org.springframework.cglib.core.internal.CustomizerRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegister.class)
@Documented
public @interface RpcScan {

    String[] basePackage();
}
