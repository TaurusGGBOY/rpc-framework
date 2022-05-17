package github.ggb.extension;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

// 对于JVM：只有C++的Bootstrap加载器 和 Java的其他类加载器
// 对于开发人员有三种：启动类加载器(C++) 拓展类加载器 应用程序加载器/系统类加载器
@Slf4j
public class ExtensionLoader<T> {
    // 看了下 要动态加载的类放在这个目录下 emmm 但是看了下 里面只是保存了一个路径 真正的内容还是在路径所在的java文件夹
    // 那这么搞意义是啥……
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    // 需要注意的是 在java中 List<Integer> List<Long>是不同的类 但是在某些语言是同一个类
    // 这里直接用类对象做key，然后value是它的加载器
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    // 然后类对象搞了个单例 作用还未知
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();
    // 感觉是给每个类 都要创建一个本类 所以type是不一样的 这个影响后面的文件名
    private final Class<?> type;
    // 他是volatile的 这说明了什么呢？
    // TODO 这个holder属实没看懂
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    // TODO这个也是
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    // 上面type处说了
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    // 也可以直接用静态方法得到 不用构造函数
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        // 空就throw
        if (null == type) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        // 如果不是接口就报错？
        // TODO 为什么要是接口……
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        // TODO 为什么要注解@SPI？
        if (null == type.getAnnotation(SPI.class)) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI.");
        }
        // 从map中获取这个type的加载器
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        // 如果是空 就putIfAbsent
        // 为什么不直接简写先putIfAbsent？ 再直接get就行？不用判null啊……
        if (null == extensionLoader) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            // 这里不会有线程安全问题？在这里删了咋办 后面的get不就是null？
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    public T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (null == clazz) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (null == instance) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    // 有个缓存的holder 里面存的的是名字和类的映射
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        // 双检测懒汉单例
        // 为啥不用static来做…… 感觉是加载比较麻烦？必须要
        if (null == classes) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (null == classes) {
                    classes = new HashMap<>();
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private void loadDirectory(Map<String, Class<?>> classes) {
        // 加载本type下的所有的类？
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            // 看起来这里的加载器就是用的本类的加载器 应该是属于第三种？
            // TODO 断点看看这个classloader的名字
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            // 这里是获取所有符合条件的 一个文件里面可能有多个要加载的
            urls = classLoader.getResources(fileName);
            if (null != urls) {
                // 遍历结果
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    loadResource(classes, classLoader, url);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    // 加载单个资源
    // 单行是这样的 zk=github.javaguide.registry.zk.ZkServiceRegistryImpl
    private void loadResource(Map<String, Class<?>> classes, ClassLoader classLoader, URL url) {
        // 不用管理close
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                // 用来当做终结符
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() <= 0) {
                    return;
                }
                try {
                    final int ei = line.indexOf('=');
                    // 比如 zk 后面的类名是包名+类名
                    String name = line.substring(0, ei).trim();
                    String clazzName = line.substring(ei + 1).trim();
                    if (name.length() > 0 && clazzName.length() > 0) {
                        // 直接包名+类名不用指定路径就可以加载吗？
                        Class<?> clazz = classLoader.loadClass(clazzName);
                        classes.put(name, clazz);
                    }
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获得这个类的单例实例
    public T getExtension(String name) {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }

        // holder的单例 但是这里没有双检测……
        Holder<Object> holder = cachedInstances.get(name);
        if (null == holder) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }

        // 对holder里面的instance的双检测……
        Object instance = holder.get();
        if (null == instance) {
            synchronized (holder) {
                instance = holder.get();
                if (null == instance) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }
}