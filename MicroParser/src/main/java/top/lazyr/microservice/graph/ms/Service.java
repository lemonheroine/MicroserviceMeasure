package top.lazyr.microservice.graph.ms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.lazyr.microservice.graph.svc.InternalGraph;
import top.lazyr.microservice.model.Api;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/4/22
 */
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class Service {
    /* 服务名字 */
    private String name;
    /**
     * {
     *      url: "/a/b/c",
     *      method: "GET"
     * }
     * ->
     * a.b.ApiFile
     */
    private Map<Api, String> api2FileName;
    /**
     * 逻辑调用边
     * inSvc.inFile -> outSvc.outFile
     * 若无调用关系，则为size=0的list
     */
    private List<LogicEdge> logicCallEdges;
    /* 传入权重 */
    private int afferentWeight;
    /* 内部依赖图 */
    private InternalGraph graph;

    public Service(String name, InternalGraph graph) {
        this.name = name;
        this.graph = graph;
        this.logicCallEdges = new ArrayList<>();
        this.api2FileName = new HashMap<>();
    }

    /**
     * 添加调用依赖关系，可以自己调用自己
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param inFileName
     */
    public boolean addCall(String inFileName, String outSvcName, String outFileName) {
        for (LogicEdge logicCallEdge : logicCallEdges) {
            if (isCalled(logicCallEdge, inFileName, outSvcName, outFileName)) {
                logicCallEdge.increaseWeight();
                return true;
            }
        }

        LogicEdge logicEdge = new LogicEdge(name, inFileName, outSvcName, outFileName);
        return logicCallEdges.add(logicEdge);
    }

    public void addApi(Set<Api> apis, String apiFileName) {
        if (apis == null) {
            return;
        }
        for (Api api : apis) {
            api2FileName.put(api, apiFileName);
        }
    }

    private boolean isCalled(LogicEdge logicCallEdge, String inFileName, String outSvcName, String outFileName) {
        return logicCallEdge.getInFileName().equals(inFileName) &&
                logicCallEdge.getOutSvcName().equals(outSvcName) &&
                logicCallEdge.getOutFileName().equals(outFileName);
    }

    /**
     * - 若对应Api存在，则返回对应ApiFileName
     * - 若对应Api不存在，则返回null
     * @param api
     * @return
     */
    public String getApiFileName(Api api) {
        return api2FileName.get(api);
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
}
