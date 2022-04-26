package github.ggb.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcServiceConfig {

    private String version = "";

    private String group = "";

    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();}

    // TODO
    public String  getServiceName(){
        return this.service.getClass().getInterfaces()[0].getCanonicalName();}

}