package github.ggb.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelProvider {
    // 每个地址设置一个channel
    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    // 每个地址存了一个channel 所以是按照hostname port ip三元组确定是不是同一个连接
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (!channelMap.containsKey(key)) {
            return null;
        }
        Channel channel = channelMap.get(key);
        if (null != channel && channel.isActive()) {
            return channel;
        }
        channelMap.remove(key);
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("after remove channel map size :[{}]", channelMap.size());
    }
}