package top.lazyr.microservice_structure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/5/15
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpCallEdge {
    private String inSvcName;
    private String inClassName;
    private String inCompleteFuncName;
    private String outSvcName;
    private String outClassName;
    private String outCompleteFuncName;
    private int weight;

    public OpCallEdge(String inSvcName, String inClassName, String inCompleteFuncName, String outSvcName, String outClassName, String outCompleteFuncName) {
        this.inSvcName = inSvcName;
        this.inClassName = inClassName;
        this.inCompleteFuncName = inCompleteFuncName;
        this.outSvcName = outSvcName;
        this.outClassName = outClassName;
        this.outCompleteFuncName = outCompleteFuncName;
        this.weight = 1;
    }

    /**
     * 权重加1
     */
    public void increaseWeight() {
        this.weight += 1;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpCallEdge that = (OpCallEdge) o;
        return Objects.equals(inSvcName, that.inSvcName) && Objects.equals(inClassName, that.inClassName) && Objects.equals(inCompleteFuncName, that.inCompleteFuncName) && Objects.equals(outSvcName, that.outSvcName) && Objects.equals(outClassName, that.outClassName) && Objects.equals(outCompleteFuncName, that.outCompleteFuncName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inSvcName, inClassName, inCompleteFuncName, outSvcName, outClassName, outCompleteFuncName);
    }

    @Override
    public String toString() {
        return inSvcName + "(" + inClassName + "." + inCompleteFuncName + ")" + "==" + weight + "==>" + outSvcName + "(" + outClassName + "." + outCompleteFuncName  + ")";
    }

}
