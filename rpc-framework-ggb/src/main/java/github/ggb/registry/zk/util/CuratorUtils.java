package github.ggb.registry.zk.util;

import github.ggb.enums.RpcConfigEnum;
import github.ggb.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
// 网飞开源的zk客户端
public final class CuratorUtils {
    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    // import here
    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    public CuratorUtils() {
    }

    // 创建持久化结点
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            // 如果本地缓存已经有了 或者 zkclient查有了
            if (REGISTERED_PATH_SET.contains(path) || null != zkClient.checkExists().forPath(path)) {
                log.info("the node already exists. The node is: [{}]", path);
            } else {
                // 否则就要创建
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("the node was created successfully. The node is:[{}]", path);
            }
            // 加到本地 也就是说 本地大家缓存服务都是不同的
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    // 获取服务的子结点
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcSerciveName) {
        // 本地有了就不查了
        // TODO 感觉有不一致的问题？ 这个交给后面断线部分处理？
        if (SERVICE_ADDRESS_MAP.containsKey(rpcSerciveName)) {
            return SERVICE_ADDRESS_MAP.get(rpcSerciveName);
        }
        List<String> result = new ArrayList<>();
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcSerciveName;
        try {
            // 如果查到了有 就注册监听
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcSerciveName, result);
            registerWatcher(rpcSerciveName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    // 这是析构那个地方搞的
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        // 并行删除本地址注册的服务
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    public static CuratorFramework getZkClient() {
        // 127.0.0.1 2181
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = DEFAULT_ZOOKEEPER_ADDRESS;
        if (null != properties && null != properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue())) {
            zookeeperAddress = properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue());
        }
        if (null != zkClient && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // curator：馆长的意思 sleep 1s 最大尝试3次 也就是最多尝试3s
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 连接地址 规则 build
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        // 这里开始连的zk非阻塞 应该在后台有线程跑了
        zkClient.start();
        try {
            // 如果阻塞超过30s 就报错
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    // 注册watcher
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        // 客户端+服务地址
        // 看源码的话 只有删除和数据变动会通知watcher
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        // 创建监听做什么
        PathChildrenCacheListener pathChildrenCacheListener = (CuratorFramework, pathChildrenCacheEvent) -> {
            // 获取该路径下所有服务地址
            List<String> serviceAddresses = CuratorFramework.getChildren().forPath(servicePath);
            // 放到本地
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        // 添加监听
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        // 开始监听
        pathChildrenCache.start();
    }
}