package github.ggb.remoting.dto;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcRequest  implements Serializable {
    private static final long serialVersionUID = 479778180175895435L;
    // 随机的
    private String requestId;
    // 接口是类的意思
    private String interfaceName;
    // 方法名字
    private String methodName;
    // 参数
    private Object[] parameters;
    private Class<?>[] paramTypes;
    // 要求同一个version
    private String version;
    // 要求同一个group
    private String group;
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}