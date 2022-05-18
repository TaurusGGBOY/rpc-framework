package github.ggb;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class Hello implements Serializable {
    private String message;
    private String description;
}