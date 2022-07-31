package top.lazyr.microservice_structure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/5/15
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Svc {
    /* 服务名字 */
    private String name;
    /**
     * {
     *      url: "/a/b/c",
     *      method: "GET"
     * }
     * ->
     * a.b.ApiClass
     */
    private List<Operation> ops;
    private List<OpCallEdge> callEdges;
    private int afferentWeight;

    public Svc(String name) {
        this.name = name;
        this.ops = new ArrayList<>();
        this.callEdges = new ArrayList<>();
        this.afferentWeight = 0;
    }

    public boolean addCall(String inClassName, String inCompleteFuncName, String outSvcName, String outClassName, String outCompleteFuncName) {
        for (OpCallEdge callEdge : callEdges) {
            if (isCalled(callEdge, name, inClassName, inCompleteFuncName, outSvcName, outClassName, outCompleteFuncName)) {
                callEdge.increaseWeight();
                return true;
            }
        }
        OpCallEdge callEdge = new OpCallEdge(name, inClassName, inCompleteFuncName, outSvcName, outClassName, outCompleteFuncName);
        return callEdges.add(callEdge);
    }

    public void addOps(Set<Operation> ops) {
        if (ops == null) {
            return;
        }
        for (Operation op : ops) {
            if (!this.ops.contains(op)) {
                this.ops.add(op);
            }
        }
    }

    private boolean isCalled(OpCallEdge callEdge, String inSvcName, String inClassName, String inCompleteFuncName, String outSvcName, String outClassName, String outCompleteFuncName) {
        return callEdge.getInSvcName().equals(inSvcName) &&
                callEdge.getInClassName().equals(inClassName) &&
                callEdge.getInCompleteFuncName().equals(inCompleteFuncName) &&
                callEdge.getOutSvcName().equals(outSvcName) &&
                callEdge.getOutClassName().equals(outClassName) &&
                callEdge.getOutCompleteFuncName().equals(outCompleteFuncName);

    }

    /**
     * 传入依赖权重加一
     */
    public void increaseAfferent() {
        this.afferentWeight++;
    }
}
