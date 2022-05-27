package github.ggb.loadbalance.loadbalancer;

import github.ggb.loadbalance.AbstractLoadBalance;
import github.ggb.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private final ConcurrentHashMap<String, ConsistenHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 定义了会调用你的hashcode 没有就如下
        // 不管有没有定义hashcode 给你算一个可重复的hashcode
        int id = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // 这里考虑了获取的名字相同 id可能不同的情况
        ConsistenHashSelector selector = selectors.get(rpcServiceName);
        // 为什么会出现id不一样的情况 说明是rpcname相同 但是id不同
        // 那么问题来了 原来的挤走了 咋办 就不处理了？顶
        if (null == selector || selector.id != id) {
            // 给每个服务都创建一个
            selectors.put(rpcServiceName, new ConsistenHashSelector(serviceAddresses, 160, id));
            selector = selectors.get(rpcServiceName);
        }
        // 搞一些关键字来把选择打散
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistenHashSelector {

        // 其实是存在一个map里面的 相当于是一个链表的作用
        private final TreeMap<Long, String> virtualInvokers;

        // 用id标志不同的服务
        private final int id;

        ConsistenHashSelector(List<String> invokers, int replicaNumber, int id) {
            this.virtualInvokers = new TreeMap<>();
            this.id = id;

            // 先撒40个虚拟节点……
            // 说实话有点多……
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int k = 0; k < 4; k++) {
                        long m = hash(digest, k);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }

        }

        // 撒在0~42亿的位置
        private long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        private byte[] md5(String key) {
            //这个感觉属实造轮子了……不用这个 随便搞个hashcode就行
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        public String select(String rpcServiceKey) {
            // 打散 这个感觉可以随便写……
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        private String selectForKey(long hashcode) {
            // 选最近的一个 方向是顺时针
            Map.Entry<Long, String> entry = virtualInvokers.ceilingEntry(hashcode);
            // 没有就回到第一个
            if (null == entry) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }
}