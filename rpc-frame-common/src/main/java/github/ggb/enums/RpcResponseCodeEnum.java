package github.ggb.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    SUCCESS(200, "Rpc success"),
    FAIL(500, "Rpc fail");
    private final int code;
    private final String message;
}
