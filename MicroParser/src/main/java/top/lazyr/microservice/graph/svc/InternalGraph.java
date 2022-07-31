package top.lazyr.microservice.graph.svc;

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
 * @created 2022/4/22
 */
@Builder
@Data
@AllArgsConstructor
public class InternalGraph {
    private static Logger logger = LoggerFactory.getLogger(InternalGraph.class);
    private List<FileNode> apiFiles;
    private List<FileNode> cellFiles;
    private List<FileNode> feignFiles;
    private Map<String, FileNode> nodeMap;

    public InternalGraph() {
        this.apiFiles = new ArrayList<>();
        this.cellFiles = new ArrayList<>();
        this.feignFiles = new ArrayList<>();
        this.nodeMap = new HashMap<>();
    }

    public InternalGraph(List<FileNode> fileNodes) {
        this();
        if (fileNodes == null) {
            logger.warn("the param(fileNodes) is null.");
            return;
        }
        for (FileNode fileNode : fileNodes) {
            addFileNode(fileNode);
        }
    }

    /**
     * 在inFile中添加一条type类型调用边，指向outFile
     * 设置outFile中的afferentWeight加一
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param inFile
     * @param outFile
     */
    public boolean addCall(FileNode inFile, FileNode outFile, String type) {
        if (inFile == null || outFile == null) {
            return false;
        }
//        if (!type.equals(FileNode.TYPE_API) ||
//                !type.equals(FileNode.TYPE_CELL) ||
//                !type.equals(FileNode.TYPE_FEIGN)) {
//            return false;
//        }
        if (inFile.equals(outFile)) { // 不添加自己依赖自己的Node
            return false;
        }

        boolean succeed = inFile.addCall(outFile, type);
        if (succeed) { // 若添加成功
            outFile.increaseAfferent();
        }
        return succeed;
    }

    /**
     * 若inNode和outNode为不同粒度，则不进行操作
     * 删除inNode中的一条depend边
     * 设置outNode中的afferentNum减一
     * - 返回false，表示不存在该边，无法删除
     * - 返回true，表示存在该边，已删除
     * @param inFile
     * @param inFile
     */
    public boolean removeCall(FileNode inFile, FileNode outFile, String type) {
        if (inFile == null || inFile == null) {
            return false;
        }
        if (!type.equals(FileNode.TYPE_API) ||
                !type.equals(FileNode.TYPE_CELL) ||
                !type.equals(FileNode.TYPE_FEIGN)) {
            return false;
        }

        if (inFile.equals(outFile)) { // 不添加自己依赖自己的Node
            return false;
        }

        boolean succeed = inFile.removeCall(outFile, type);
        if (succeed) { // 若添加成功
            outFile.decayAfferent();
        }
        return succeed;
    }

    /**
     * 返回所有项目外的Api文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterNonSystemApiFiles() {
        return filterFiles(apiFiles, false);
    }

    /**
     * 返回所有项目内的Cell文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterSystemCellFiles() {
        return filterFiles(cellFiles, true);
    }

    /**
     * 返回所有项目外的Cell文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterNonSystemCellFiles() {
        return filterFiles(cellFiles, false);
    }

    /**
     * 返回所有项目内的Feign文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterSystemFeignFiles() {
        return filterFiles(feignFiles, true);
    }

    /**
     * 返回所有项目外的Feign文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterNonSystemFeignFiles() {
        return filterFiles(feignFiles, false);
    }

    /**
     * 返回所有项目内的Api文件
     * - 若结果为空，则返回null
     * @return
     */
    public List<FileNode> filterSystemApiFiles() {
        return filterFiles(apiFiles, true);
    }

    /**
     * 返回符合条件的FileNode集合
     * 若无符合条件的FileNode，则返回null
     * @param fileNodes
     * @param isSystem
     * @return
     */
    private List<FileNode> filterFiles(List<FileNode> fileNodes, boolean isSystem) {
        List<FileNode> filterNodes = new ArrayList<>();
        for (FileNode fileNode : fileNodes) {
            if (fileNode.isSystem() == isSystem) {
                filterNodes.add(fileNode);
            }
        }
        return filterNodes.size() == 0 ? null : filterNodes;
    }

    /**
     * 查找name为fileName的FileNode
     * - 若未查到则返回null
     * @param fileName
     * @return
     */
    public FileNode findFileByName(String fileName) {
        return nodeMap.get(fileName);
    }

    /**
     * 添加fileNode，若fileNode已存在，则不添加
     * - 返回false，表示fileNode已存在、fileNode为null或fileNode类型错误，未添加到graph中
     * - 返回true，表示fileNode成功添加到graph中
     * @param fileNode
     */
    public boolean addFileNode(FileNode fileNode) {
        if (fileNode == null) {
            return false;
        }
        if (nodeMap.containsKey(fileNode.getName())) { // 若已存在，则拒绝添加
            return false;
        }
        nodeMap.put(fileNode.getName(), fileNode);

        switch (fileNode.getType()) {
            case FileNode.TYPE_API:
                apiFiles.add(fileNode);
                break;
            case FileNode.TYPE_CELL:
                cellFiles.add(fileNode);
                break;
            case FileNode.TYPE_FEIGN:
                feignFiles.add(fileNode);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * 删除fileNodes中fileNode
     * 删除nodeMap中value为fileNode的键值对
     * - 返回false，表示fileNode为null或不存在或类型错误
     * - 返回true，表示fileNode存在并删除
     * @param fileNode
     */
    public boolean removeFileNode(FileNode fileNode) {
        if (fileNode == null) {
            return false;
        }
        nodeMap.remove(fileNode.getName());
        switch (fileNode.getType()) {
            case FileNode.TYPE_API:
                apiFiles.remove(fileNode);
                break;
            case FileNode.TYPE_CELL:
                cellFiles.remove(fileNode);
                break;
            case FileNode.TYPE_FEIGN:
                feignFiles.remove(fileNode);
                break;
            default:
                return false;
        }
        return true;
    }

    public boolean removeFileNode(String fileName) {
        return removeFileNode(nodeMap.get(fileName));
    }

    public List<FileNode> getAllFileNodes() {
        List<FileNode> fileNodes = new ArrayList<>();
        fileNodes.addAll(apiFiles);
        fileNodes.addAll(cellFiles);
        fileNodes.addAll(feignFiles);
        return fileNodes;
    }
}
