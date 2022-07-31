package top.lazyr.microservice.graph.svc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/4/22
 */
@Builder
@Data
@AllArgsConstructor
public class FileNode {
    /* 文件名 */
    private String name;
    /* 文件来源 */
    private String from;
    /* 文件类型 */
    private String type;
    /* 调用边，若无调用关系，则为size=0的list */
    private List<Edge> callEdges;
    /* 传入权重 */
    private int afferentWeight;

    public static final String FROM_SYSTEM = "SYSTEM";
    public static final String FROM_NON_SYSTEM = "NON_SYSTEM";

    public static final String TYPE_API = "API";
    public static final String TYPE_FEIGN = "FEIGN";
    public static final String TYPE_CELL = "CELL";

    public FileNode(String name, String from, String type) {
        this.name = name;
        this.from = from;
        this.type = type;
        this.callEdges = new ArrayList<>();
        this.afferentWeight = 0;
    }

    /**
     * 添加调用依赖关系
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param outNode
     * @param type
     */
    public boolean addCall(FileNode outNode, String type) {
//        System.out.println("addCall");
        if (outNode == null) {
            System.out.println(outNode + " is not exist.");
            return false;
        }
        for (Edge callEdge : callEdges) {
            if (callEdge.getOutFile().equals(outNode.getName())) { // 若调用关系已存在
                callEdge.increaseWeight(type);
                return true;
            }
        }
        // 若调用关系不存在
        Edge callEdge = new Edge(name, outNode.getName(), type);
//        System.out.println(callEdge);
        return callEdges.add(callEdge);
    }

    /**
     * 删除callEdges中指向outNode且类型为type的边
     * - 返回false，表示不存在该边，无法删除
     * - 返回true，表示存在该边，已删除
     * @param outNode
     * @param type
     * @return
     */
    public boolean removeCall(FileNode outNode, String type) {
        if (outNode == null) {
            return false;
        }

        Edge deletedEdge = null;
        for (Edge callEdge : callEdges) {
            if (callEdge.getOutFile().equals(outNode.name)) { // 删除调用边中的类型
                callEdge.decayWeight(type);
                deletedEdge = callEdge;
                break;
            }
        }

        // 删除完类型，边的权重减为0，则删除该边
        if (deletedEdge != null && deletedEdge.getTotalWeight() != 0) {
            return callEdges.remove(deletedEdge);
        }

        return true;
    }

    /**
     * 传入依赖权重加一
     */
    public void increaseAfferent() {
        this.afferentWeight++;
    }

    /**
     * 传入依赖权重减一
     */
    public void decayAfferent() {
        this.afferentWeight--;
    }

    /**
     * 是否为系统内的文件
     * @return
     */
    public boolean isSystem() {
        return from.equals(FROM_SYSTEM);
    }

    /**
     * 是否为API文件
     * @return
     */
    public boolean isApi() {
        return type.equals(TYPE_API);
    }

    /**
     * 是否为FEIGN文件
     * @return
     */
    public boolean isFeign() {
        return type.equals(TYPE_FEIGN);
    }

    /**
     * 是否为CELL文件
     * @return
     */
    public boolean isCell() {
        return type.equals(TYPE_CELL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNode node = (FileNode) o;
        return Objects.equals(name, node.name) && Objects.equals(from, node.from) && Objects.equals(type, node.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, from, type);
    }

    @Override
    public String toString() {
        int totalWeight = 0;
        for (Edge callEdge : callEdges) {
            totalWeight += callEdge.getTotalWeight();
        }

        return "FileNode{" +
                "name='" + name + '\'' +
                ", from='" + from + '\'' +
                ", type='" + type + '\'' +
                ", afferentWeight=" + afferentWeight +
                ", callFileNum=" + totalWeight +
                '}';
    }
}
