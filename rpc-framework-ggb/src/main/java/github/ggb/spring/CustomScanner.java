package github.ggb.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

public class CustomScanner extends ClassPathBeanDefinitionScanner {
    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }

    public CustomScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        super(registry);
        // 添加包含的过滤器
        // 添加注解类型过滤 注解的Class
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }
}