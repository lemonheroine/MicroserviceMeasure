package top.lazyr.microserviceName_structure.fieldcall.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyr
 * @created 2022/5/13
 */
@Builder
@Data
@AllArgsConstructor
public class FieldCallGraph {
    private static Logger logger = LoggerFactory.getLogger(FieldCallGraph.class);
    private List<MethodNode> methodNodes;
    private List<FieldNode> fieldNodes;
    private Map<String, MethodNode> methodNodeMap;
    private Map<String, FieldNode> fieldNodeMap;

    public FieldCallGraph() {
        this.methodNodes = new ArrayList<>();
        this.fieldNodes = new ArrayList<>();
        this.methodNodeMap = new HashMap<>();
        this.fieldNodeMap = new HashMap<>();
    }

    public boolean addCall(MethodNode inMethodNode, FieldNode outFieldNode) {
        if (inMethodNode == null || outFieldNode == null) {
            return false;
        }

        // 若记录自己
        if (inMethodNode.getBelongClassName()
                .equals(outFieldNode.getBelongClassName())) { // 不添加自己依赖自己的Node
            return false;
        }
        boolean succeed = inMethodNode.addCall(outFieldNode);
        if (succeed) {
            outFieldNode.increaseAfferent();
        }
        return succeed;
    }

    public boolean addMethodNode(MethodNode methodNode) {
        if (methodNode == null) {
            return false;
        }
        if (methodNodeMap.containsKey(methodNode.getId())) { // 若已存在，则拒绝添加
            return false;
        }
        methodNodeMap.put(methodNode.getId(), methodNode);
        return methodNodes.add(methodNode);
    }

    public boolean addFieldNode(FieldNode fieldNode) {
        if (fieldNode == null) {
            return false;
        }
        if (fieldNodeMap.containsKey(fieldNode.getId())) { // 若已存在，则拒绝添加
            return false;
        }
        fieldNodeMap.put(fieldNode.getId(), fieldNode);
        return fieldNodes.add(fieldNode);
    }


    public MethodNode findMethodNodeById(String id) {
        return methodNodeMap.get(id);
    }

    public FieldNode findFieldNodeById(String id) {
        return fieldNodeMap.get(id);
    }


}
