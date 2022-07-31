package top.lazyr.microservice.graph.ms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/4/22
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogicEdge {
    private String inSvcName;
    private String inFileName;
    private String outSvcName;
    private String outFileName;
    private int weight;

    public LogicEdge(String inSvcName, String inFileName, String outSvcName, String outFileName) {
        this.inSvcName = inSvcName;
        this.inFileName = inFileName;
        this.outSvcName = outSvcName;
        this.outFileName = outFileName;
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
        LogicEdge logicEdge = (LogicEdge) o;
        return Objects.equals(inSvcName, logicEdge.inSvcName) && Objects.equals(inFileName, logicEdge.inFileName) && Objects.equals(outSvcName, logicEdge.outSvcName) && Objects.equals(outFileName, logicEdge.outFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inSvcName, inFileName, outSvcName, outFileName);
    }

    @Override
    public String toString() {
        return inSvcName + "(" + inFileName + ")" + "==" + weight + "==>" + outSvcName + "(" + outFileName + ")";
    }
}
