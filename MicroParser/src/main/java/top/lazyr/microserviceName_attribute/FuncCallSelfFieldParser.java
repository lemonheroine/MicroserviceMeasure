package top.lazyr.microserviceName_attribute;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lazyr.microserviceName_attribute.model.FuncCallSelfFieldInfo;
import top.lazyr.manager.CtClassManager;
import top.lazyr.microserviceName_structure.classinfo.ClassInfoParser;
import top.lazyr.util.ClsUtil;
import top.lazyr.util.SCUtil;
import top.lazyr.util.TypeUtil;
import top.lazyr.validator.VarValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class FuncCallSelfFieldParser {
    private static Logger logger = LoggerFactory.getLogger(ClassInfoParser.class);
    private CtClassManager ctClassManager;

    public FuncCallSelfFieldParser() {
        this.ctClassManager = CtClassManager.getCtClassManager();
    }

    public List<FuncCallSelfFieldInfo> parseFunc(String svcPath, boolean isApi) {
        List<FuncCallSelfFieldInfo> funcCallSelfFieldInfos = new ArrayList<>();
        List<CtClass> ctClasses = ctClassManager.extractCtClass(svcPath);
        if (VarValidator.empty(ctClasses)) {
            logger.info( svcPath + " 路径下无class文件");
            return funcCallSelfFieldInfos;
        }
        // 获取funcCallSelfFieldInfos基础信息
        Map<CtMethod, FuncCallSelfFieldInfo> ctMethodMap = extractCtMethodMap(ctClasses, isApi);

        if (VarValidator.empty(ctMethodMap)) {
            logger.info("{} 中{}无{}", svcPath, isApi ? "接口" : "实体类", isApi ? "操作" : "方法");
        }
        buildCallSelfFields(ctMethodMap);

        for (CtMethod ctMethod : ctMethodMap.keySet()) {
            funcCallSelfFieldInfos.add(ctMethodMap.get(ctMethod));
        }

        return funcCallSelfFieldInfos;
    }

    private Map<CtMethod, FuncCallSelfFieldInfo> extractCtMethodMap(List<CtClass> ctClasses, boolean isApi) {
        Map<CtMethod, FuncCallSelfFieldInfo> ctMethodMap = new HashMap<>();
        for (CtClass ctClass : ctClasses) {
            boolean isApiClass = SCUtil.isApiClass(ctClass);
            CtMethod[] ctMethods = null;
            if ((isApi && isApiClass) || (!isApi && !isApiClass)) {
                ctMethods = CtClassManager.getAllCtMethods(ctClass);
            }
            if (VarValidator.empty(ctMethods)) {
                continue;
            }
            for (CtMethod ctMethod : ctMethods) {
                boolean isApiFunc = SCUtil.isApiFunc(ctMethod);
                if ((isApi && isApiClass && isApiFunc) || (!isApi && !isApiClass)) {
                    FuncCallSelfFieldInfo funcCallSelfFieldInfo = buildFuncCallSelfFieldInfo(ctMethod, ctClass);
                    if (funcCallSelfFieldInfo == null) {
                        continue;
                    }
                    ctMethodMap.put(ctMethod, funcCallSelfFieldInfo);
                }
            }
        }
        return ctMethodMap;
    }

    private void buildCallSelfFields(Map<CtMethod, FuncCallSelfFieldInfo> ctMethodMap) {
        for (CtMethod ctMethod : ctMethodMap.keySet()) {
            try {
                FuncCallSelfFieldInfo funcCallSelfFieldInfo = ctMethodMap.get(ctMethod);
                ctMethod.instrument(new SelfFieldCallBuilder(funcCallSelfFieldInfo));
            } catch (CannotCompileException e) {
//                throw new RuntimeException(e);
                logger.error("解析方法 {} 失败", ctMethod.getLongName());
            }
        }
    }


    private FuncCallSelfFieldInfo buildFuncCallSelfFieldInfo(CtMethod ctMethod, CtClass belongClass) {
        if (ctMethod == null || belongClass == null) {
            return null;
        }
        FuncCallSelfFieldInfo funcCallSelfFieldInfo = null;
        List<String> paramTypes = new ArrayList<>();
        String typeName = "";
        try {
            CtClass[] paramClasses = ctMethod.getParameterTypes();
            if (VarValidator.notEmpty(paramClasses)) {
                for (CtClass paramClass : paramClasses) {
                    paramTypes.add(TypeUtil.unbox(paramClass.getName()));
                }
            }
        } catch (NotFoundException e) { // 参数为项目外类
            String[] paramClassNames = e.getMessage().split(",");
            for (String paramClassName : paramClassNames) {
                paramTypes.add(paramClassName);
            }
        }
        try {
            typeName = ctMethod.getReturnType().getName();
        } catch (NotFoundException e) { // 返回值为项目外类
            typeName = e.getMessage();
        }
        funcCallSelfFieldInfo = FuncCallSelfFieldInfo.builder()
                .methodModifier(ClsUtil.toModifierStr(ctMethod.getModifiers()))
                .methodName(ctMethod.getName())
                .paramClassNames(paramTypes)
                .returnClassName(typeName.equals("void") ? "void" : TypeUtil.unbox(typeName))
                .isApiClass(SCUtil.isApiClass(belongClass))
                .isApiFunc(SCUtil.isApiFunc(ctMethod))
                .belongClassName(belongClass.getName())
                .belongClassModifier(ClsUtil.toModifierStr(belongClass.getModifiers()))
                .belongClassType(ClsUtil.toTypeStr(belongClass.getModifiers()))
                .selfFields(new ArrayList<>())
                .build();
        return funcCallSelfFieldInfo;
    }

    private class SelfFieldCallBuilder extends ExprEditor {
        private FuncCallSelfFieldInfo funcCallSelfFieldInfo;

        public SelfFieldCallBuilder(FuncCallSelfFieldInfo funcCallSelfFieldInfo) {
            this.funcCallSelfFieldInfo = funcCallSelfFieldInfo;
        }

        @Override
        public void edit(FieldAccess f) throws CannotCompileException {
            String fieldName = f.getFieldName();
            String className = f.getClassName();
            String belongClassName = funcCallSelfFieldInfo.getBelongClassName();
            if (belongClassName.equals(className)) {
                List<String> selfFields = funcCallSelfFieldInfo.getSelfFields();
                if (!selfFields.contains(fieldName)) {
                    selfFields.add(TypeUtil.unbox(Descriptor.toString(f.getSignature())));
                }
            }
        }
    }
}
