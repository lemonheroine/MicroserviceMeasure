package top.lazyr.microservice_structure.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.validator.VarValidator;

import java.util.*;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class MS {
    private static Logger logger = LoggerFactory.getLogger(MS.class);
    private List<Svc> svcs;
    private Map<String, Svc> svcMap;

    public MS() {
        this.svcs = new ArrayList<>();
        this.svcMap = new HashMap<>();
    }

    public boolean addCall(Svc inSvc, String inClassName, String inCompleteName, Svc outSvc, String outClassName, String outCompleteName) {
        boolean succeed = inSvc.addCall(inClassName, inCompleteName, outSvc.getName(), outClassName, outCompleteName);
        if (succeed) {
            outSvc.increaseAfferent();
        }
        return succeed;
    }

    public Svc findSvcByName(String svcName) {
        return svcMap.get(svcName);
    }

    public boolean addSvc(Svc svc) {
        if (svc == null) {
            return false;
        }
        if (svcMap.containsKey(svc.getName())) { // 若已存在，则拒绝添加
            return false;
        }
        svcMap.put(svc.getName(), svc);
        return svcs.add(svc);
    }

    public List<Svc> getSvcs() {
        return svcs;
    }

    public Operation getOutOpByApiAndSvc(String svcName, Agency agency) {
        Svc svc = svcMap.get(svcName);
        if (svc == null) {
            logger.error("svc({}) is not exist.", svcName);
            return null;
        }
        Set<Operation> outOps = agency.getOps();
        List<Operation> ops = svc.getOps();
        if (VarValidator.empty(ops)) {
            return null;
        }
        for (Operation op : ops) {
            for (Operation outOp : outOps) {
                if (outOp.getMethod().equals(op.getMethod()) && outOp.getUrl().equals(op.getUrl())) {
                    return op;
                }
            }
        }
        return null;
    }
}
