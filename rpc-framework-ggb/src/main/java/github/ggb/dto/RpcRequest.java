package github.ggb.dto;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 7971559871565960710L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    // TODO 这个是干什么的
    private String group;

    public String getRPCServiceName(){
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}