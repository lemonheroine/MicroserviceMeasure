package top.lazyr.microservice.manager;

import top.lazyr.microservice.graph.svc.FileNode;

/**
 * @author lazyr
 * @created 2022/4/22
 */
public class GraphManager {
    /**
     * 构建 (name=fileName) && (from=SYSTEM) && (type=API)  的FileNode
     * @return
     */
    public static FileNode buildSystemApiFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_SYSTEM, FileNode.TYPE_API);
        return node;
    }

    /**
     * 构建 (name=fileName) && (from=NON_SYSTEM) && (type=API)  的FileNode
     * @return
     */
    public static FileNode buildNonSystemApiFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_NON_SYSTEM, FileNode.TYPE_API);
        return node;
    }

    /**
     * 构建 (name=fileName) && (from=SYSTEM) && (type=CELL)  的FileNode
     * @return
     */
    public static FileNode buildSystemCellFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_SYSTEM, FileNode.TYPE_CELL);
        return node;
    }

    /**
     * 构建 (name=fileName) && (from=NON_SYSTEM) && (type=CELL)  的FileNode
     * @return
     */
    public static FileNode buildNonSystemCellFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_NON_SYSTEM, FileNode.TYPE_CELL);
        return node;
    }

    /**
     * 构建 (name=fileName) && (from=SYSTEM) && (type=FEGIN)  的FileNode
     * @return
     */
    public static FileNode buildSystemFeignFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_SYSTEM, FileNode.TYPE_FEIGN);
        return node;
    }

    /**
     * 构建 (name=fileName) && (from=NON_SYSTEM) && (type=FEGIN)  的FileNode
     * @return
     */
    public static FileNode buildNonSystemFeignFile(String fileName) {
        FileNode node = new FileNode(fileName, FileNode.FROM_NON_SYSTEM, FileNode.TYPE_FEIGN);
        return node;
    }

}
