package top.lazyr.microserviceName_structure.fieldcall.model;

import javassist.CtMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.manager.CtClassManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/5/13
 */
@Builder
@Data
@AllArgsConstructor
public class MethodNode {
    private static Logger logger = LoggerFactory.getLogger(MethodNode.class);
    private String belongClassName;
    private String completeMethodName;
    private boolean isSystem;
    private boolean isApiClass;
    private boolean isApiFunc;
    private String id;
    private List<FieldCallEdge> callEdges;

    public MethodNode(String belongClassName, CtMethod ctMethod, boolean isSystem, boolean isApiClass, boolean isApiFunc) {
        this.belongClassName = belongClassName;
        this.completeMethodName = CtClassManager.buildCompleteMethodName(ctMethod);
        this.isSystem = isSystem;
        this.isApiClass = isApiClass;
        this.isApiFunc = isApiFunc;
        this.callEdges = new ArrayList<>();
        this.id = this.belongClassName + "." + this.completeMethodName;;
    }

    public MethodNode(String belongClassName, String completeMethodName, boolean isSystem, boolean isApiClass, boolean isApiFunc) {
        this.belongClassName = belongClassName;
        this.completeMethodName = completeMethodName;
        this.isSystem = isSystem;
        this.isApiClass = isApiClass;
        this.isApiFunc = isApiFunc;
        this.callEdges = new ArrayList<>();
        this.id = this.belongClassName + "." + this.completeMethodName;
    }

    /**
     * 添加调用依赖关系
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param outFiledNode
     * @return
     */
    public boolean addCall(FieldNode outFiledNode) {
        if (outFiledNode == null) {
            logger.info("outMethodNode is not exist.");
            return false;
        }
        for (FieldCallEdge callEdge : callEdges) {
            if (callEdge.isOutField(outFiledNode)) { // 若调用关系已存在
                callEdge.increaseWeight();
                return true;
            }
        }
        // 若调用关系不存在
        FieldCallEdge callEdge = new FieldCallEdge(this.id, outFiledNode.getBelongClassName(), outFiledNode.getFieldName());
        return callEdges.add(callEdge);

    }

    public boolean isSystem() {
        return isSystem;
    }

    public boolean isApi() {
        return isApiClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodNode that = (MethodNode) o;
        return isSystem == that.isSystem && isApiClass == that.isApiClass && Objects.equals(belongClassName, that.belongClassName) && Objects.equals(completeMethodName, that.completeMethodName) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(belongClassName, completeMethodName, isSystem, isApiClass, id);
    }
}
