package top.lazyr.microservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author lazyr
 * @created 2021/11/28
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Api {
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
        Api api = (Api) o;
        return Objects.equals(method, api.method) && Objects.equals(url, api.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, url);
    }

    @Override
    public String toString() {
        return "Api{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
