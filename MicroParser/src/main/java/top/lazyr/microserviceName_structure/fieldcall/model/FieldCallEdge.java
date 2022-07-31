package top.lazyr.microserviceName_structure.fieldcall.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lazyr
 * @created 2022/5/13
 */
@Builder
@Data
@AllArgsConstructor
public class FieldCallEdge {
    private static Logger logger = LoggerFactory.getLogger(FieldCallEdge.class);
    private String inMethodId;
    private String outClassName;
    private String outFieldName;
    private String outFieldId;
    private int weight;

    public FieldCallEdge(String inMethodId, String outClassName, String outFieldName) {
        this.inMethodId = inMethodId;
        this.outClassName = outClassName;
        this.outFieldName = outFieldName;
        this.outFieldId = outClassName + "." + outFieldName;
        this.weight = 1;
    }

    public void increaseWeight() {
        this.weight++;
    }

    public void decayWeight() {
        this.weight--;
    }

    public boolean isOutField(FieldNode outFieldNode) {
        return outFieldId.equals(outFieldNode.getId());
    }

    @Override
    public String toString() {
        return inMethodId + " ==" + weight + "==> " + outFieldId;
    }
}
