package top.lazyr.microservice_structure.model;

/**
 * @author lazyr
 * @created 2022/5/15
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.lazyr.validator.VarValidator;

import java.util.*;

/**
 * 中介类
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agency {
    private String className;
    private String completeFuncName;

    private String svcName;

    private String id;

    private Set<Operation> ops;


    public Agency(String svcName, String className, String completeFuncName) {
        this.className = className;
        this.completeFuncName = completeFuncName;
        this.svcName = svcName;
        this.id = className + "." + completeFuncName;
        this.ops = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agency agency = (Agency) o;
        return Objects.equals(className, agency.className) && Objects.equals(completeFuncName, agency.completeFuncName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, completeFuncName);
    }

    public void setOperations(Set<Operation> ops) {
        if (VarValidator.empty(ops)) {
            return;
        }
        this.ops = ops;
    }
}
