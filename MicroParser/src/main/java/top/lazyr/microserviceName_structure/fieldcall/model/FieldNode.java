package top.lazyr.microserviceName_structure.fieldcall.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/5/13
 */
@Builder
@Data
@AllArgsConstructor
public class FieldNode {
    private static Logger logger = LoggerFactory.getLogger(FieldNode.class);
    private String belongClassName;
    private String fieldName;
    private String id;
    private int afferentWeight;

    public FieldNode(String belongClassName, String fieldName) {
        this.belongClassName = belongClassName;
        this.fieldName = fieldName;
        this.id = this.belongClassName + "." + this.fieldName;
        this.afferentWeight = 0;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldNode fieldNode = (FieldNode) o;
        return Objects.equals(belongClassName, fieldNode.belongClassName) && Objects.equals(fieldName, fieldNode.fieldName) && Objects.equals(id, fieldNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(belongClassName, fieldName, id);
    }
}
