package github.ggb.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcServiceConfig {

    private String version = "";

    // 感主要用于处理一个接口有多个类实现的情况。
    private String group = "";

    private Object service;

    public String getRpcServiceName() {
        // 这个是有问题的 必须要有分隔符
        return this.getServiceName() + this.getGroup() + this.getVersion();}

    public String  getServiceName(){
        // 所以服务名字其实就是这个服务的实现的接口的规范名称
        return this.service.getClass().getInterfaces()[0].getCanonicalName();}

}