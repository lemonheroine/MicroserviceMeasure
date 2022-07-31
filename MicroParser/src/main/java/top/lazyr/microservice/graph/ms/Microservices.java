package top.lazyr.microservice.graph.ms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microservice.model.Api;

import java.util.*;

/**
 * @author lazyr
 * @created 2021/11/22
 */
public class Microservices {
    private static Logger logger = LoggerFactory.getLogger(Microservices.class);
    private List<Service> services;
    private Map<String, Service> svcMap;

    public Microservices() {
        this.services = new ArrayList<>();
        this.svcMap = new HashMap<>();
    }


    /**
     * 在inSvc中添加一条inSvc.inFile -> outFile.outFile 的逻辑边
     * 可以自己调用自己
     * 设置outFile中的afferentWeight加一
     *  - 返回false，表示未添加
     *  - 返回true，表示已添加
     * @param inSvc
     * @param inFile
     * @param outSvc
     * @param outFile
     * @return
     */
    public boolean addCall(Service inSvc, String inFile, Service outSvc, String outFile) {
        if (inSvc == null || outSvc == null) {
            return false;
        }
        boolean succeed = inSvc.addCall(inFile, outSvc.getName(), outFile);
        if (succeed) {
            outSvc.increaseAfferent();
        }
        return succeed;
    }

    public Service findSvcByName(String svcName) {
        return svcMap.get(svcName);
    }

    public boolean addService(Service service) {
        if (service == null) {
            return false;
        }
        if (svcMap.containsKey(service.getName())) { // 若已存在，则拒绝添加
            return false;
        }
        svcMap.put(service.getName(), service);
        return services.add(service);
    }

    public String getOutFileByApiAndSvc(String svcName, Set<Api> apis) {
        Service service = svcMap.get(svcName);
        if (service == null) {
            logger.error("{} is not exist.", svcName);
            return null;
        }

        for (Api api : apis) {
            String apiFileName = service.getApiFileName(api);
            if (apiFileName != null) {
                return apiFileName;
            }
        }


        return null;
    }

    public List<Service> getServices() {
        return services;
    }
}

