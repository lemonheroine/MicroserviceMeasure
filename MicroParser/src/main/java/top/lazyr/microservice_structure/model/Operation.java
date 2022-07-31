package top.lazyr.microservice_structure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author lazyr
 * @created 2022/5/15
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Operation {
    private String className;
    private String completeFuncName;
    private String method;
    private String url;
    public static final String GET = "GET";
    public static final String PATCH = "PATCH";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String POST = "POST";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation operation = (Operation) o;
        return Objects.equals(className, operation.className) && Objects.equals(completeFuncName, operation.completeFuncName) && Objects.equals(method, operation.method) && Objects.equals(url, operation.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, completeFuncName, method, url);
    }

    @Override
    public String toString() {
        return "Operation{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", className='" + className + '\'' +
                ", completeFuncName='" + completeFuncName + '\'' +
                '}';
    }
}
