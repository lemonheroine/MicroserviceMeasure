package top.lazyr.microserviceName_structure.classinfo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/12
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MethodInfo {
    /* 方法名 */
    private String methodName;
    /**
     * 返回值类名
     * - 若返回值为void，则为""
     * - 若返回值不为void，则为具体的类名
     */
    private String returnClassName;
    /**
     * 参数类名列表
     * - 若无参数，则为size=0的list
     * - 若有参数，则为参数对应类名的list
     */
    private List<String> paramClassNames;

    /* 方法修饰符: default; public; private; protect */
    private String methodModifier;

    /* 所位于类的类名 */
    private String belongClassName;

    /* 所位于类的类型: interface; abstract; normal */
    private String belongClassType;

    /* 所位于类的修饰符: default; public; private; protect */
    private String belongClassModifier;

    /* 是否为Api方法 */
    private boolean isApiFunc;

    /* 是否为Api方法 */
    private boolean isApiClass;

    public String getCompleteName() {
        return "";
    }
}
