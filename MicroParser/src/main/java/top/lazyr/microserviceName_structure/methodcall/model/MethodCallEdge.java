package top.lazyr.microserviceName_structure.methodcall.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author lazyr
 * @created 2022/5/13
 */
@Builder
@Data
@AllArgsConstructor
public class MethodCallEdge {
    private String inClassName;
    private String inCompleteMethodName;
    private String inMethodId;
    private String outClassName;
    private String outCompleteMethodName;
    private String outMethodId;
    private int weight;

    public MethodCallEdge(String inClassName, String inCompleteMethodName, String outClassName, String outCompleteMethodName) {
        this.inClassName = inClassName;
        this.inCompleteMethodName = inCompleteMethodName;
        this.outClassName = outClassName;
        this.outCompleteMethodName = outCompleteMethodName;
        this.weight = 1;
        this.inMethodId = this.inClassName + "." + this.inCompleteMethodName;
        this.outMethodId = this.outClassName + "." + this.outCompleteMethodName;
    }



    public void increaseWeight() {
        this.weight++;
    }

    public void decayWeight() {
        this.weight--;
    }

    public boolean isOutMethod(MethodNode outMethodNode) {
        return outMethodId.equals(outMethodNode.getId());
    }

    @Override
    public String toString() {
        return inMethodId + " ==" + weight + "==> " + outMethodId;
    }
}
