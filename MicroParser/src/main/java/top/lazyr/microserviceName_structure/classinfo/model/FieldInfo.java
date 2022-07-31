package top.lazyr.microserviceName_structure.classinfo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lazyr
 * @created 2022/5/12
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldInfo {
    /* 字段名 */
    private String fieldName;
    /* 字段类名 */
    private String className;
    /* 字段修饰符: default; public; private; protect */
    private String modifier;
    /* 所位于类的类名 */
    private String belongClassName;
    /* 所位于类的类型: interface; abstract; normal */
    private String belongClassType;
    /* 所位于类的修饰符: default; public; private; protect */
    private String belongClassModifier;
    /* 所位于的类是否为Api类 */
    private boolean isApiClass;


//    public String getCompleteName() {
//    }
}
