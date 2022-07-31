package top.lazyr.microservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyr
 * @created 2022/4/22
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Feign {

    private String feignFileName;

    private String svcName;

    /**
     * a.b.c.func(java.lang.String) -> {
     *                                     method : "GET",
     *                                     url : "/a/b/c"
     *                                  }
     */
    private Map<String, Set<Api>> func2Api;

    public Feign(String feignFileName, String svcName) {
        this.feignFileName = feignFileName;
        this.svcName = svcName;
        this.func2Api = new HashMap<>();
    }

    public void putApi(String funcName, Set<Api> apis) {
        this.func2Api.put(funcName, apis);
    }

    @Override
    public String toString() {
        return "Feign{" +
                "feignFileName='" + feignFileName + '\'' +
                ", svcName='" + svcName + '\'' +
                ", func2Api=" + func2Api +
                '}';
    }
}
