import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MIF {

    Map<String, MIF.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;
    int entitiesIndex = 0;
    int interRelationIndex = 0;
    int operationIndex = 0;

    public static void main(String[] args) throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data/";

//        String projectLocation = "157-black-shop";
//        String projectLocation = "61-microservice-recruit-master";
//        String projectLocation = "63-springcloud-course";
//        String projectLocation = "65-microservice_arch_springcloud";
//        String projectLocation = "70-SpringCloudDemo";
//        String projectLocation = "76-cangjingge";
//        String projectLocation = "77-momo-cloud-permission";
//        String projectLocation = "97-mall-swarm";
//        String projectLocation = "116-spring-boot-microservices";
//        String projectLocation = "123-madao_service";
//        String projectLocation = "131-mall-cloud-alibaba";
//        String projectLocation = "165-mall4cloud";
//        String projectLocation = "170-microservices-event-sourcing";
//        String projectLocation = "178-sc";
//        String projectLocation = "185-RuoYi-Cloud";
//        String projectLocation = "186-micro-service-springcloud";
//        String projectLocation = "190-wanxin-p2p";
//        String projectLocation = "195-iclyj-cloud";
//        String projectLocation = "199-light-reading-cloud";
//        String projectLocation = "230-food-ordering-backend-system";
        String projectLocation = "238-springcloud-oauth2";
//        String projectLocation = "241-simplemall";
//        String projectLocation = "260-SpringCloud-MSA";
//        String projectLocation = "Gitee-2-springcloud2";
//        String projectLocation = "Gitee-3-zscat_sw";
//        String projectLocation = "Gitee-4-sc";
//        String projectLocation = "Gitee-8-microservices-platform";
//        String projectLocation = "Gitee-13-Snowy-Cloud";
//        String projectLocation = "Gitee-14-基于SpringCloud微服务实现的互联网招聘平台";
//        String projectLocation = "Gitee-18-rpush";
//        String projectLocation = "Gitee-20-grocery-micro-service";
//        String projectLocation = "Gitee-21-tangdao-master";
//        String projectLocation = "Gitee-22-LibraPlatform";


        String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
        List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

        new MIF().calculateMicroservicesMIF(projectPrefix+projectLocation+"/",microservicesList,microservicesDependencies);
    }

    public double calculateMicroservicesMIF(String projectPath, List<String>  microservicesList,String microservicesDependencies){
        //先读取每个微服务结构数据
        for (String microserviceName: microservicesList){
            if (!microserviceMap.containsKey(microserviceName)){
                MIF.Microservice microservice = new MIF.Microservice(microserviceName);
                microserviceMap.put(microserviceName,microservice);
                microservice2IndexMap.put(microserviceName,microserviceIndex);
                index2MicroserviceMap.put(microserviceIndex++,microserviceName);
                readASingleMicroservice(microservice,projectPath+microserviceName+"_structure.xlsx");
                handleClassMethodRelationship(microservice,projectPath+microserviceName+"_structure.xlsx");
                handleInterface2ClassRelationship(microservice,projectPath+microserviceName+"_structure.xlsx");
                handleClass2InterfaceRelationship(microservice,projectPath+microserviceName+"_structure.xlsx");
            }
        }
        //再读取微服务之间的结构数据
        handleMicroservicesRelationship(projectPath+microservicesDependencies);

        double MIF = 0;
        double[][] MIF2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录MIF指标的矩阵
        double[][] Ca2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ca基础指标的矩阵
        double[][] Ce2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ce基础指标的矩阵

        int allOperationsCount = 0;
        for(MIF.Microservice microservice1: microserviceMap.values()){
            double MIFaMicroservice = 0;
            int operationsCount = microservice1.getOperationsCount();
            allOperationsCount += operationsCount;
            int microservice1Index = microservice2IndexMap.get(microservice1.name);
            int allEntitiesCount = 0;
            int CaMicroservice = 0;
            int CeMicroservice = 0;
            int AISMicroservice = 0;
            int ADSMicroservice = 0;
            System.out.println("size of microservice "+ microservice1.name + " is " +microservice1.index);
            System.out.println("operation size of microservice "+ microservice1.name + " is " +operationsCount);


            for(MIF.Microservice microservice2: microserviceMap.values()){
                // 计算MIF指标
                if (microservice1.equals(microservice2))continue;
                int entitiesCount = microservice2.index;
                allEntitiesCount += entitiesCount;
                double MIF2Microservices = calculateTwoMicroservicesMIF(microservice1, microservice2);
//                System.out.println("MIF value between microservices "+ microservice1.name + " and " + microservice2.name + " is: "+ MIF2Microservices);

                MIFaMicroservice += entitiesCount * MIF2Microservices;

                int microservice2Index = microservice2IndexMap.get(microservice2.name);
                MIF2MicroservicesMatrix[microservice1Index][microservice2Index] = MIF2Microservices;

                // 计算Ca指标
                Ca2MicroservicesMatrix[microservice1Index][microservice2Index] = calculateTwoMicroservicesCa(microservice1, microservice2);
                CaMicroservice += Ca2MicroservicesMatrix[microservice1Index][microservice2Index];
                // 计算Ce指标
                Ce2MicroservicesMatrix[microservice1Index][microservice2Index] = calculateTwoMicroservicesCe(microservice1, microservice2);
                CeMicroservice += Ce2MicroservicesMatrix[microservice1Index][microservice2Index];

                //根据Ca矩阵计算AIS
                if (Ca2MicroservicesMatrix[microservice1Index][microservice2Index]!=0){
                    AISMicroservice++;
                }
                //根据Ce矩阵计算ADS
                if (Ce2MicroservicesMatrix[microservice1Index][microservice2Index]!=0){
                    ADSMicroservice++;
                }

            }
            if (allEntitiesCount==0)System.out.println("Error Microservices size");
            else MIFaMicroservice /= allEntitiesCount;
            System.out.println("Absolute Importance of the Service AIS of microservice " + microservice1.name + " is: "+ AISMicroservice);
            System.out.println("Absolute Dependence of the Service ADS " + microservice1.name + " is: "+ ADSMicroservice);
            System.out.println("Afferent Coupling Ca of microservice " + microservice1.name + " is: "+ CaMicroservice);
            System.out.println("Efferent Coupling Ce of microservice " + microservice1.name + " is: "+ CeMicroservice);
            System.out.println("Afferent MIFa value of microservice "+ microservice1.name + " is: "+ MIFaMicroservice);
            System.out.println("----------------------------------------------------------");

            MIF += operationsCount * MIFaMicroservice;
            entitiesIndex += microservice1.index;
        }

        operationIndex = allOperationsCount;
        if(allOperationsCount == 0)System.out.println("Error Microservices operation ");
        else MIF /= allOperationsCount;

        //计算传入依赖带来的微服务间影响MIF
        for (int i =0;i<microserviceIndex;i++){
            double MIFeMicroservice = 0;
            for (int j=0;j<microserviceIndex;j++){
                if (j==i)continue;
                MIFeMicroservice += MIF2MicroservicesMatrix[j][i] * microserviceMap.get(index2MicroserviceMap.get(j)).getOperationsCount();
            }
            MIFeMicroservice /= allOperationsCount;
            System.out.println("Efferent MIFe value of microservice "+ index2MicroserviceMap.get(i) + " is: "+ MIFeMicroservice);

        }

        System.out.println("Microservices count of this system is: "+ microserviceIndex);
        System.out.println("Entities count of this system is: "+ entitiesIndex);
        System.out.println("Operation count of this system is: "+ operationIndex);
        System.out.println("InterRelationIndex count of this system is: "+ interRelationIndex);
        System.out.println("MIF value of this system is: "+ MIF);

        return MIF;
    }

    public int calculateTwoMicroservicesCa(MIF.Microservice microservice1,MIF.Microservice microservice2){
        // 计算microservice2中有多少个类依赖microservice1
        HashSet<MIF.Class> reachableEntities = new HashSet<>();
        for (MIF.Interface interface1: microservice1.getInterfaceMap().values()){
            for (MIF.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                        String className = interInvokerMethods.get(i).className;
                        MIF.Class invokingClass = microservice2.getClassMap().get(className);
                        reachableEntities.add(invokingClass);// 将遍历到的类放入set
                    }
                }
            }
        }
        return reachableEntities.size();
    }

    public int calculateTwoMicroservicesCe(MIF.Microservice microservice1,MIF.Microservice microservice2){
        // 计算microservice2中有多少个接口被microservice1依赖
        HashSet<MIF.Interface> reachableInterfaces = new HashSet<>();
        for (MIF.Interface interface1: microservice2.getInterfaceMap().values()){
            for (MIF.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice1.name)) {// 该接口操作被当前微服务所依赖
                        reachableInterfaces.add(interface1);
                    }
                }
            }
        }
        return reachableInterfaces.size();
    }

    public double calculateTwoMicroservicesMIF(MIF.Microservice microservice1,MIF.Microservice microservice2){
        double MIF = 0;
        int operationsCount = microservice1.getOperationsCount();
        if(operationsCount == 0) return 0;
            for (MIF.Interface interface1: microservice1.getInterfaceMap().values()){
            for (MIF.Method operation: interface1.getMethodMap().values()){
                double MIFOperation2Microservice = calculateOperation2MicroserviceMIF(operation, microservice2);
//                System.out.println("MIF value between operation "+ operation.name +" in "+ microservice1.name + " and " + microservice2.name + " is: "+ MIFOperation2Microservice);
                MIF += MIFOperation2Microservice;
            }
        }
        return MIF/operationsCount;
    }

    public double calculateOperation2MicroserviceMIF(MIF.Method operation, MIF.Microservice microservice2){
        int entities = microservice2.getEntities();
        HashSet<MIF.Class> reachableEntities = new HashSet<>();

        ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
        int distance = 1 ;
        int accumulatedDistance[] = new int[1];
        if (interInvokerMethods.size()==0)return (double)1/entities;// 一个接口到一个微服务的影响因子MIF的最小值为1/#AllEntities
        else{//遍历和microservice2相关的调用
            for(int i=0; i<interInvokerMethods.size(); i++){
                if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)){// 仅处理与当前微服务相关的实体方法
                    String className = interInvokerMethods.get(i).className;
                    MIF.Class invokingClass = microservice2.getClassMap().get(className);
                    reachableEntities.add(invokingClass);// 将遍历到的类放入set
                    accumulatedDistance[0] += distance; //所有直接依赖该operation的方法到该operation的路径为1

                    //获得调用当前方法的方法
                    String methodName = interInvokerMethods.get(i).methodName;
                    MIF.Method invokingMethod = invokingClass.methodMap.get(methodName);
                    putIntraInvokerEntities(distance, accumulatedDistance, microservice2,invokingClass, invokingMethod,reachableEntities);
                }
            }
        }
        double MIF = (double)reachableEntities.size()/entities + (double)1/(entities*accumulatedDistance[0]+entities);
//        System.out.println("MIF from operation "+ operation.name +  " to microservice "+ microservice2.name+ " is: "+ MIF);
        return MIF;
    }

    public void putIntraInvokerEntities(int distance, int accumulatedDistance[], MIF.Microservice microservice2,MIF.Class currentClass, MIF.Method method,HashSet<MIF.Class> reachableEntities){
        //获得调用调用方法的方法列表
//        System.out.println("error: current microservice is " + microservice2.name +  "+ method");
        ArrayList<IntraMicroserviceInvoker> intraInvokerMethods = method.getIntraInvokerMethods();
        if (intraInvokerMethods.size() == 0) return;
        distance++;
        for(int j=0; j < intraInvokerMethods.size(); j++){
            String className = intraInvokerMethods.get(j).className;
            MIF.Class invokingClass = microservice2.getClassMap().get(className);

            //当调用调用方法的类不为调用方法的类时添加该类
            if (!invokingClass.equals(currentClass)){
                reachableEntities.add(invokingClass);// 将遍历到的类放入set
                accumulatedDistance[0] += distance;
            }

            //获得调用方法
            String methodName = intraInvokerMethods.get(j).methodName;
            MIF.Method invokingMethod = invokingClass.methodMap.get(methodName);
            putIntraInvokerEntities(distance, accumulatedDistance,microservice2,invokingClass, invokingMethod,reachableEntities);
        }
    }

    public void readASingleMicroservice(MIF.Microservice microservice, String fileName){

        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MIF.Method method = new MIF.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Class(className,method);
        }

        //读取接口方法表信息
        List<List<String>> interfaceMethodList=Util.readExcel(fileName,5,4);
        for(List<String> list:interfaceMethodList){
            if(list.get(0).equals(""))
                continue;
            String interfaceName = list.get(0);
            microservice.addInterface(interfaceName);
            microservice.addClass(interfaceName);//把接口当作类处理

            if(list.get(1).equals(""))
                continue;
            MIF.Method method = new MIF.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);
            microservice.addMethod2Class(interfaceName,method);//把接口当作类处理
        }
    }

    public void handleClassMethodRelationship(MIF.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            MIF.Class class1=microservice.getClassMap().get(list.get(0));
            MIF.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleInterface2ClassRelationship(MIF.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,5);
        for(List<String> list:relationshipList){
            //MIF.Interface interface1=microservice.getInterfaceMap().get(list.get(0));
            MIF.Class class1=microservice.getClassMap().get(list.get(0));//把接口当作类处理
            MIF.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的接口到类方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleClass2InterfaceRelationship(MIF.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,7);
        for(List<String> list:relationshipList){
            MIF.Class class1=microservice.getClassMap().get(list.get(0));
            //MIF.Interface interface2=microservice.getInterfaceMap().get(list.get(2));
            MIF.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的类到接口操作间调用关系记到被调用操作的内部list中
           // microservice.getInterfaceMap().get(list.get(2)).putIntraMethodInvoker(list.get(3),list.get(0),list.get(1));
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleMicroservicesRelationship(String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,7,0);
        for(List<String> list:relationshipList){
            MIF.Microservice microservice1 = microserviceMap.get(list.get(0));
            MIF.Microservice microservice2 = microserviceMap.get(list.get(3));

            if (microservice1==null||microservice2==null)
                continue;

            int index1= microservice2IndexMap.get(list.get(0));
            int index2= microservice2IndexMap.get(list.get(3));

            // 判断是否是来自同一个微服务的调用
            if (index1 == index2) System.out.println("invoking from the same microservices!!!");
            else interRelationIndex++;
            //将每个遍历到的方法到接口操作间调用关系记到被调用操作的内部list中
            microservice2.getInterfaceMap().get(list.get(4)).putInterMethodInvoker(list.get(5),list.get(0),list.get(1),list.get(2));
        }
    }


    class Microservice{
        String name;
        Map<String, MIF.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index; //包含普通类和接口

        Map<String, MIF.Interface> interfaceMap;
        Map<String,Integer> interface2IndexMap;
        Map<Integer,String> index2InterfaceMap;
        int interfaceIndex;

        public Microservice(String name){
            this.name = name;
            this.classMap = new HashMap<>();
            this.class2IndexMap = new HashMap<>();
            this.index2ClassMap = new HashMap<>();
            this.index =0;

            this.interfaceMap = new HashMap<>();
            this.interface2IndexMap = new HashMap<>();
            this.index2InterfaceMap =  new HashMap<>();
            this.interfaceIndex = 0;
        }

        public Map<String, MIF.Class> getClassMap() {
            return classMap;
        }

        public Map<String, MIF.Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new MIF.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, MIF.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new MIF.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, MIF.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }

        public int getOperationsCount(){
            int count = 0;
            for (MIF.Interface interface1: this.interfaceMap.values()){
                count += interface1.getOperations();
            }
            return count;
        }

        public int getEntities(){
            return this.index;
        }
    }

    class Class {
        String name;
        int publicUnitCount;
        int protectedUnitCount;
        int privateUnitCount;
        Map<String,MIF.Method> methodMap =new HashMap<>();


        public Class(String name) {
            this.name = name;
            this.publicUnitCount = 0;
            this.protectedUnitCount = 0;
            this.privateUnitCount = 0;
        }


        public void addMethod(MIF.Method a) {
            methodMap.put(a.name,a);
            if (a.modifier.equals("public"))
                publicUnitCount++;
            if (a.modifier.equals("protected"))
                protectedUnitCount++;
            if (a.modifier.equals("private"))
                privateUnitCount++;
        }

        public void putMethodInvoker(String invokedMethodName, String className, String methodName){
            if(this.methodMap.containsKey(invokedMethodName)){
                this.methodMap.get(invokedMethodName).putIntraInvokerMethod(className,methodName);
            }
        }

    }

    class Method{
        String name;
        String parameter;
        String returnType;
        String modifier;
//        Boolean isOperation;

        ArrayList<IntraMicroserviceInvoker> intraInvokerMethods = new ArrayList<>();
        ArrayList<InterMicroserviceInvoker> interInvokerMethods = new ArrayList<>();

        //目前来看并没有parameter属性，因为该属性被包含在了name里
        public Method(String name,String modifier,String returnType,String parameter){
            this.name = name;
            this.parameter =parameter;
            this.returnType = returnType;
            this.modifier = modifier;
//            this.isOperation = isOperation;
        }

        public void putIntraInvokerMethod(String className,String methodName){
            this.intraInvokerMethods.add(new IntraMicroserviceInvoker(className,methodName));
        }

        public void putInterInvokerMethod(String microserviceName, String className,String methodName){
            this.interInvokerMethods.add(new InterMicroserviceInvoker(microserviceName,className,methodName));
        }

        public ArrayList<InterMicroserviceInvoker> getInterInvokerMethods(){
            return this.interInvokerMethods;
        }

        public ArrayList<IntraMicroserviceInvoker> getIntraInvokerMethods(){
            return this.intraInvokerMethods;
        }
    }

    class IntraMicroserviceInvoker{
        String className;
        String methodName;
        public IntraMicroserviceInvoker(String className,String methodName){
            this.className = className;
            this.methodName = methodName;
        }
    }

    class InterMicroserviceInvoker{
        String microserviceName;
        String className;
        String methodName;
        public InterMicroserviceInvoker(String microserviceName,String className,String methodName){
            this.microserviceName = microserviceName;
            this.className = className;
            this.methodName = methodName;
        }
    }

    class Interface{
        String name;
        Map<String,MIF.Method> methodMap =new HashMap<>();
        int publicMethodCount;
        int protectedMethodCount;

        public Interface(String name){
            this.name = name;
            this.publicMethodCount = 0;
            this.protectedMethodCount = 0;
        }

        public void addMethod(MIF.Method m){
            methodMap.put(m.name,m);
            if(m.modifier.equals("public"))
                publicMethodCount++;
            if(m.modifier.equals("protected"))
                protectedMethodCount++;
        }

        public int getOperations(){
            return this.protectedMethodCount + this.publicMethodCount;
        }

        public Map<String, MIF.Method> getMethodMap() {
            return methodMap;
        }

        public void putInterMethodInvoker(String invokedMethodName, String microserviceName,String className, String methodName){
            if(this.methodMap.containsKey(invokedMethodName)){
                this.methodMap.get(invokedMethodName).putInterInvokerMethod(microserviceName,className,methodName);
            }
        }

        public void putIntraMethodInvoker(String invokedMethodName, String className, String methodName){
            if(this.methodMap.containsKey(invokedMethodName)){
                this.methodMap.get(invokedMethodName).putIntraInvokerMethod(className,methodName);
            }
        }

    }
}
