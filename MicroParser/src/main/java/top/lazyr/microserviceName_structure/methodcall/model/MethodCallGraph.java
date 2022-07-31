package top.lazyr.microserviceName_structure.methodcall.model;

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
public class MethodCallGraph {
    private static Logger logger = LoggerFactory.getLogger(MethodCallGraph.class);
    private List<MethodNode> systemMethodNode;
    private List<MethodNode> nonSystemMethodNode;
    private Map<String, MethodNode> nodeMap;

    public MethodCallGraph() {
        this.systemMethodNode = new ArrayList<>();
        this.nonSystemMethodNode = new ArrayList<>();
        this.nodeMap = new HashMap<>();
    }

    /**
     * 在inFile中添加一条type类型调用边，指向outFile
     * 设置outFile中的afferentWeight加一
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param inMethodNode
     * @param outMethodNode
     * @return
     */
    public boolean addCall(MethodNode inMethodNode, MethodNode outMethodNode) {
        if (inMethodNode == null || outMethodNode == null) {
            return false;
        }
        if (inMethodNode.equals(outMethodNode)) { // 不添加自己依赖自己
            return false;
        }
        boolean succeed = inMethodNode.addCall(outMethodNode);
        if (succeed) {
            outMethodNode.increaseAfferent();
        }
        return succeed;
    }

    /**
     * 添加fileNode，若fileNode已存在，则不添加
     * - 返回false，表示fileNode已存在、fileNode为null或fileNode类型错误，未添加到graph中
     * - 返回true，表示fileNode成功添加到graph中
     * @param methodNode
     * @return
     */
    public boolean addMethodNode(MethodNode methodNode) {
        if (methodNode == null) {
            return false;
        }
        if (nodeMap.containsKey(methodNode.getId())) { // 若已存在，则拒绝添加
            return false;
        }
        nodeMap.put(methodNode.getId(), methodNode);
        if (methodNode.isSystem()) {
            systemMethodNode.add(methodNode);
        } else {
            nonSystemMethodNode.add(methodNode);
        }
        return true;
    }

    public MethodNode findMethodNodeById(String id) {
        return nodeMap.get(id);
    }
}
