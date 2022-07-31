package top.lazyr.util;

import cn.hutool.core.util.StrUtil;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microservice.model.Api;
import top.lazyr.microservice_structure.model.Operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SpringCloud相关功能
 * @author lazyr
 * @created 2022/4/22
 */
public class SCUtil {
    private static Logger logger = LoggerFactory.getLogger(SCUtil.class);

    /**
     * 判断ctClass是否有@FeignClient注解
     * @param ctClass
     * @return
     */
    public static boolean isFeign(CtClass ctClass) {
        if (ctClass == null) {
            return false;
        }

        FeignClient feignClient = null;

        try {
            feignClient = (FeignClient) ctClass.getAnnotation(FeignClient.class);
        } catch (ClassNotFoundException e) {
            logger.error("pares class feign annotation failed: " + e.getMessage());
        }

        return feignClient != null;
    }

    /**
     * 判断ctClass是否有@Controller相关接口
     * @param ctClass
     * @return
     */
    public static boolean isApiClass(CtClass ctClass) {
        if (ctClass == null) {
            return false;
        }

        Controller controller = null;
        RestController restController = null;
        ResponseBody responseBody = null;
        RequestMapping requestMapping = null;
        try {
            controller = (Controller)ctClass.getAnnotation(Controller.class);
            restController = (RestController)ctClass.getAnnotation(RestController.class);
            responseBody = (ResponseBody)ctClass.getAnnotation(ResponseBody.class);
            requestMapping = (RequestMapping)ctClass.getAnnotation(RequestMapping.class);
        } catch (ClassNotFoundException e) {
            logger.error("pares class api annotation failed: " + e.getMessage());
        }

        return controller != null || restController != null || responseBody != null || requestMapping != null;
    }

    /**
     * 若ctClass有@FeignClient注解，则返回注解中name属性或value属性的值
     * 若ctClass无@FeignClient注解，则返回""
     * @param ctClass
     * @return
     */
    public static String extractSvcName(CtClass ctClass) {
        FeignClient feignClient = null;
        String svcName = "";
        try {
            feignClient = (FeignClient) ctClass.getAnnotation(FeignClient.class);
        } catch (ClassNotFoundException e) {
            logger.error("pares class feign annotation failed: " + e.getMessage());
        }
        if (feignClient == null) { // 若该类无FeignClient注解
            return "";
        }
        svcName = feignClient.value();
        if (svcName.equals("")) {
            svcName = feignClient.name();
        }
        return svcName.replace(":", "_");
    }

    /**
     * 返回结果是以 "/" 开头
     * @param ctClass
     * @return
     */
    public static String extractFeignPrefix(CtClass ctClass) {
        FeignClient feignClient = null;
        String prefix = "";
        try {
            feignClient = (FeignClient) ctClass.getAnnotation(FeignClient.class);
        } catch (ClassNotFoundException e) {
            logger.error("pares class feign annotation failed: " + e.getMessage());
        }
        if (feignClient == null) { // 若该类无FeignClient注解
            return "/";
        }
        prefix = "/" + feignClient.path();
        return uniqueBackslash(prefix);  // 将 // 替换为 /
    }

    /**
     * 将url中所有的 // 替换为 /
     * @param url
     * @return
     */
    private static String uniqueBackslash(String url) {
        return url.replaceAll("//", "/");
    }

    /**
     * 判断ctMethod是否为接口方法
     * @param ctMethod
     * @return
     */
    public static boolean isApiFunc(CtMethod ctMethod) {
        RequestMapping requestMapping = null;
        GetMapping getMapping = null;
        PostMapping postMapping = null;
        PutMapping putMapping = null;
        DeleteMapping deleteMapping = null;
        PatchMapping patchMapping = null;
        try {
            requestMapping = (RequestMapping) ctMethod.getAnnotation(RequestMapping.class);
            getMapping = (GetMapping) ctMethod.getAnnotation(GetMapping.class);
            postMapping = (PostMapping) ctMethod.getAnnotation(PostMapping.class);
            putMapping = (PutMapping) ctMethod.getAnnotation(PutMapping.class);
            deleteMapping = (DeleteMapping) ctMethod.getAnnotation(DeleteMapping.class);
            patchMapping = (PatchMapping) ctMethod.getAnnotation(PatchMapping.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return requestMapping != null ||
                getMapping != null ||
                postMapping != null ||
                putMapping != null ||
                deleteMapping != null ||
                patchMapping != null;
    }

    /**
     * 将ctMethod转换为Api
     * - 若ctMethod非API方法，则返回size=0的Api集合
     * @param ctMethod
     * @param feignPrefix
     * @return
     */
    public static Set<Api> extractApiFromCtMethod(CtMethod ctMethod, String feignPrefix) {
        Set<Api> apis = new HashSet<>();
        if (!isApiFunc(ctMethod)) {
            return apis;
        }
        RequestMapping requestMapping = null;
        GetMapping getMapping = null;
        PostMapping postMapping = null;
        PutMapping putMapping = null;
        DeleteMapping deleteMapping = null;
        PatchMapping patchMapping = null;
        try {
            requestMapping = (RequestMapping) ctMethod.getAnnotation(RequestMapping.class);
            getMapping = (GetMapping) ctMethod.getAnnotation(GetMapping.class);
            postMapping = (PostMapping) ctMethod.getAnnotation(PostMapping.class);
            putMapping = (PutMapping) ctMethod.getAnnotation(PutMapping.class);
            deleteMapping = (DeleteMapping) ctMethod.getAnnotation(DeleteMapping.class);
            patchMapping = (PatchMapping) ctMethod.getAnnotation(PatchMapping.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String url = "";
        Set<String> methods = new HashSet<>();
        if (requestMapping != null) {
            methods = getMethodsFromRequestMapping(requestMapping);
            url = buildUrl(feignPrefix, requestMapping.value());
        }
        if (getMapping != null) {
            methods.add(Api.GET);
            url = buildUrl(feignPrefix, getMapping.value());
        }
        if (postMapping != null) {
            methods.add(Api.POST);
            url = buildUrl(feignPrefix, postMapping.value());
        }
        if (putMapping != null) {
            methods.add(Api.PUT);
            url = buildUrl(feignPrefix, putMapping.value());
        }
        if (deleteMapping != null) {
            methods.add(Api.DELETE);
            url = buildUrl(feignPrefix, deleteMapping.value());
        }
        if (patchMapping != null) {
            methods.add(Api.PATCH);
            url = buildUrl(feignPrefix, patchMapping.value());
        }

        for (String method : methods) {
            Api api = Api.builder().url(uniqueBackslash(url)).method(method).build();
            apis.add(api);
        }

        return apis;
    }

    public static Set<Operation> extractOpFromCtMethod(String className, CtMethod ctMethod, String feignPrefix) {
        Set<Api> apis = extractApiFromCtMethod(ctMethod, feignPrefix);
        List<Operation> ops = ObjUtils.cast(new ArrayList<>(apis), Operation.class);
        for (Operation op : ops) {
            op.setClassName(className);
            op.setCompleteFuncName(CtClassManager.buildCompleteMethodName(ctMethod));
        }
        return new HashSet<>(ops);
    }

    private static Set<String> getMethodsFromRequestMapping(RequestMapping requestMapping) {
        Set<String> methods = new HashSet<>();
        if (requestMapping.method() == null || requestMapping.method().length == 0) {
            methods = allMethods();
        } else {
            for (RequestMethod rm : requestMapping.method()) {
                if (rm == RequestMethod.GET) {
                    methods.add(Api.GET);
                }
                if (rm == RequestMethod.PATCH) {
                    methods.add(Api.PATCH);
                }
                if (rm == RequestMethod.PUT) {
                    methods.add(Api.PUT);
                }
                if (rm == RequestMethod.DELETE) {
                    methods.add(Api.DELETE);
                }
                if (rm == RequestMethod.POST) {
                    methods.add(Api.POST);
                }
            }
        }
        return methods;
    }

    private static Set<String> allMethods() {
        Set<String> allMethods = new HashSet<>();
        allMethods.add("GET");
        allMethods.add("PATCH");
        allMethods.add("PUT");
        allMethods.add("DELETE");
        allMethods.add("POST");
        return allMethods;
    }

    /**
     * prefix = a, values[0] = b, 返回 /a/b
     * prefix = a, values[0] = b/, 返回 /a/b
     * prefix = /a, values[0] = /b, 返回 /a/b
     * prefix = , values[0] = b, 返回 /b
     * prefix = , values[0] = , 返回 /
     * @param prefix
     * @param values
     * @return
     */
    private static String buildUrl(String prefix, String[] values) {
        String url = prefix;
        if (values != null && values.length != 0) {
            url = ("/" + prefix + "/" + values[0]).replace("//", "/");
        }
        url = "/" + StrUtil.strip(url, "/", "/");
        return url;
    }

    /**
     * 若ctClass有@RestController或@RequestMapping注解，则返回注解中value属性的值
     * 若ctClass无@RestController或@RequestMapping注解，则返回"/"
     * @param ctClass
     * @return
     */
    public static String extractApiPrefix(CtClass ctClass) {
        RestController restController = null;
        RequestMapping requestMapping = null;
        String prefix = "";
        try {
            restController = (RestController)ctClass.getAnnotation(RestController.class);
            requestMapping = (RequestMapping)ctClass.getAnnotation(RequestMapping.class);
        } catch (ClassNotFoundException e) {
            logger.error("pares class api annotation failed: " + e.getMessage());
        }
        if (restController != null) {
            prefix = restController.value();
        }

        if (requestMapping != null) {
            String[] prefixValues = requestMapping.value();
            if (prefixValues.length != 0) {
                prefix = prefixValues[0];
            }
            String[] prefixPaths = requestMapping.path();
            if (prefixPaths.length != 0) {
                prefix = prefixPaths[0];
            }
        }
        return uniqueBackslash("/" + prefix);
    }

}
