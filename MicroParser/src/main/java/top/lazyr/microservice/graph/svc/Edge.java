package top.lazyr.microservice.graph.svc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/4/22
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Edge {
    private String inFile;
    private String outFile;
    /*  调用类型 => 权重 */
    private Map<String, Integer> callWeight;
    /* 类A调用类B的方法 */
    public static final String METHOD_CALL = "MethodCall";
    /* 类A使用super()调用父类B的构造器(暂时废弃)  */
    public static final String CONSTRUCTOR_CALL = "ConstructorCall";
    /* 类A调用类B的成员变量 */
    public static final String FIELD_ACCESS = "FieldAccess";
    /* 类A使用new初始化普通类B */
    public static final String NEW_EXPR = "NewExpr";
    /* 类A使用new初始化数组 */
    public static final String NEW_ARRAY = "NewArray";
    /* 类A中使用类B进行类型转换 */
    public static final String CAST = "Cast";
    /* 类A中使用try-catch语句中使用throws语句抛出异常类B */
    public static final String HANDLER = "Handler";

    public Edge(String inNode, String outNode, String type) {
        this.inFile = inNode;
        this.outFile = outNode;
        this.callWeight = new HashMap<>();
        this.callWeight.put(type, 1);
    }

    /**
     * type类型调用关系的权重加1
     */
    public void increaseWeight(String type) {
        callWeight.put(type, callWeight.getOrDefault(type, 0) + 1);
    }

    /**
     * type类型调用关系的权重减1
     * - 若减完，权重减为0，则删除该类型调用关系
     */
    public void decayWeight(String type) {
        if (!callWeight.containsKey(type)) { // 若不存在，则拒绝删除
            return;
        }

        if (callWeight.get(type) <= 1) {
            callWeight.remove(type);
            return;
        }

        callWeight.put(type, callWeight.get(type) - 1);
    }

    /**
     * 获取type类型调用权重
     * @param type
     * @return
     */
    public int getWeightOfType(String type) {
        return callWeight.get(type) == null ? 0 : callWeight.get(type);
    }

    /**
     * 获取所有类型调用权重之和
     * @return
     */
    public int getTotalWeight() {
        int totalWeight = 0;
        for (String type : callWeight.keySet()) {
            totalWeight += callWeight.get(type);
        }
        return totalWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return inFile.equals(edge.inFile) && outFile.equals(edge.outFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inFile, outFile);
    }

    @Override
    public String toString() {
        return inFile + " ==" + getTotalWeight() + "==> " + outFile;
    }
}
