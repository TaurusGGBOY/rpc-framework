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
        // TODO
        int id = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistenHashSelector selector = selectors.get(rpcServiceName);
        if (null == selector || selector.id != id) {
            selectors.put(rpcServiceName, new ConsistenHashSelector(serviceAddresses, 160, id));
            selector = selectors.get(rpcServiceName);
        }
        // TODO ?
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistenHashSelector {

        private final TreeMap<Long, String> virtualInvokers;

        private final int id;

        ConsistenHashSelector(List<String> invokers, int replicaNumber, int id) {
            this.virtualInvokers = new TreeMap<>();
            this.id = id;

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

        private long hash(byte[] digest, int idx) {
            // TODO what?
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        private byte[] md5(String key) {
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
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        private String selectForKey(long hashcode) {
            // TODO
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashcode, true).firstEntry();
            if (null == entry) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }

    }
}