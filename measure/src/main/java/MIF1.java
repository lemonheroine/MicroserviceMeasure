import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MIF1 {

    Map<String, MIF1.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;
    int entitiesIndex = 0;
    int interRelationIndex = 0;
    int interfaceIndex = 0;

    public static void main(String[] args) throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "structure_data_0612/";

//        String projectLocation = "157-black-shop";
//        String projectLocation = "61-microservice-recruit-master";
//        String projectLocation = "63-springcloud-course";
//        String projectLocation = "65-microservice_arch_springcloud";
//        String projectLocation = "70-SpringCloudDemo";
//        String projectLocation = "76-cangjingge";
//        String projectLocation = "77-momo-cloud-permission";
//        String projectLocation = "97-mall-swarm";
//        String projectLocation = "107-msa-springcloud";
//        String projectLocation = "116-spring-boot-microservices";
//        String projectLocation = "123-madao_service";
//        String projectLocation = "130-micro-service-springcloud";
        String projectLocation = "131-mall-cloud-alibaba";
//        String projectLocation = "165-mall4cloud";
//        String projectLocation = "170-microservices-event-sourcing";
//        String projectLocation = "175-Sa-Token";
//        String projectLocation = "176-jcconf2018-microservice-with-springcloud";
//        String projectLocation = "178-sc";
//        String projectLocation = "185-RuoYi-Cloud";
//        String projectLocation = "186-micro-service-springcloud";
//        String projectLocation = "190-wanxin-p2p";
//        String projectLocation = "195-iclyj-cloud";
//        String projectLocation = "199-light-reading-cloud";
//        String projectLocation = "230-food-ordering-backend-system";
//        String projectLocation = "238-springcloud-oauth2";
//        String projectLocation = "241-simplemall";
//        String projectLocation = "260-SpringCloud-MSA";
//        String projectLocation = "Gitee-2-springcloud2";
//        String projectLocation = "Gitee-3-zscat_sw";
//        String projectLocation = "Gitee-4-sc";
//        String projectLocation = "Gitee-5-microservice-spring-cloud";
//        String projectLocation = "Gitee-8-microservices-platform";
//        String projectLocation = "Gitee-13-Snowy-Cloud";
//        String projectLocation = "Gitee-14-基于SpringCloud微服务实现的互联网招聘平台";
//        String projectLocation = "Gitee-18-rpush";
//        String projectLocation = "Gitee-20-grocery-micro-service";
//        String projectLocation = "Gitee-21-tangdao-master";
//        String projectLocation = "Gitee-22-LibraPlatform";


        String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
        List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

        new MIF1().calculateMicroservicesMIF(projectPrefix+projectLocation+"/",microservicesList,microservicesDependencies);
    }

    public void calculateMicroservicesMIF(String projectPath, List<String>  microservicesList,String microservicesDependencies){
        //先读取每个微服务结构数据
        for (String microserviceName: microservicesList){
            if (!microserviceMap.containsKey(microserviceName)){
                MIF1.Microservice microservice = new MIF1.Microservice(microserviceName);
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

        double[][] MIF2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录MIF指标的矩阵
        double[][] Ca2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ca基础指标的矩阵
        double[][] Ce2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ce基础指标的矩阵

        for(MIF1.Microservice microservice1: microserviceMap.values()){
            double MIFaMicroservice = 0;
            int microservice1Index = microservice2IndexMap.get(microservice1.name);
            int allEntitiesCount = 0;
            int CaMicroservice = 0;
            int CeMicroservice = 0;
            int AISMicroservice = 0;
            int ADSMicroservice = 0;
            System.out.println("size of microservice "+ microservice1.name + " is " +microservice1.index);
            System.out.println("interface size of microservice "+ microservice1.name + " is " +microservice1.interfaceIndex);

            for(MIF1.Microservice microservice2: microserviceMap.values()){
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

            entitiesIndex += microservice1.index;
            interfaceIndex += microservice1.interfaceIndex;
        }

        //计算传入依赖带来的微服务间影响MIF
        for (int i =0;i<microserviceIndex;i++){
            double MIFeMicroservice = 0;
            int allInterfacesCount = 0;
            for (int j=0;j<microserviceIndex;j++){
                if (j==i)continue;
                int curInterfacesCount = microserviceMap.get(index2MicroserviceMap.get(j)).interfaceIndex;
                allInterfacesCount += curInterfacesCount;
                MIFeMicroservice += MIF2MicroservicesMatrix[j][i] * curInterfacesCount;
            }
            MIFeMicroservice /= allInterfacesCount;
            System.out.println("Efferent MIFe value of microservice "+ index2MicroserviceMap.get(i) + " is: "+ MIFeMicroservice);

        }

        System.out.println("Microservices count of this system is: "+ microserviceIndex);
        System.out.println("Entities count of this system is: "+ entitiesIndex);
        System.out.println("Interfaces count of this system is: "+ interfaceIndex);
        System.out.println("InterRelationIndex count of this system is: "+ interRelationIndex);
//        System.out.println("MIF value of this system is: "+ MIF);
    }

    public int calculateTwoMicroservicesCa(MIF1.Microservice microservice1, MIF1.Microservice microservice2){
        // 计算microservice2中有多少个类依赖microservice1
        HashSet<MIF1.Class> reachableEntities = new HashSet<>();
        for (MIF1.Interface interface1: microservice1.getInterfaceMap().values()){
            for (MIF1.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                        String className = interInvokerMethods.get(i).className;
                        MIF1.Class invokingClass = microservice2.getClassMap().get(className);
                        reachableEntities.add(invokingClass);// 将遍历到的类放入set
                    }
                }
            }
        }
        return reachableEntities.size();
    }

    public int calculateTwoMicroservicesCe(MIF1.Microservice microservice1, MIF1.Microservice microservice2){
        // 计算microservice2中有多少个接口被microservice1依赖
        HashSet<MIF1.Interface> reachableInterfaces = new HashSet<>();
        for (MIF1.Interface interface1: microservice2.getInterfaceMap().values()){
            for (MIF1.Method operation: interface1.getMethodMap().values()){
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

    public double calculateTwoMicroservicesMIF(MIF1.Microservice microservice1, MIF1.Microservice microservice2){
        int interfacesCount = microservice1.interfaceIndex;
        int entitiesCount = microservice2.index;
        if (interfacesCount == 0 || entitiesCount == 0) return 0;
        int distanceTable[] = new int[entitiesCount];

        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<entitiesCount;i++) distanceTable[i] = -1;
        int reachableInterface = 0;
        // 遍历微服务1的所有操作以更新可达的微服务2中实体对应的距离表
        for (MIF1.Interface interface1: microservice1.getInterfaceMap().values()){
            Boolean interfaceReachableFlag = false;
            for (MIF1.Method operation: interface1.getMethodMap().values()){
                // 计算所有微服务2中实体到当前操作的距离并更新distance表
                if (traversableOperation2Microservice(operation,microservice2,distanceTable)) interfaceReachableFlag = true;
            }
            if (interfaceReachableFlag) reachableInterface++; //被遍历过的此接口对微服务2而言是可遍历到的
        }

        if (reachableInterface==0) return 0;
        int reachableEntities = 0;
        int accumulatedDistance = 0;
        for(int i =0; i<entitiesCount;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];
            }
        }
        if (reachableEntities == 0) return 0;

        double MIF = ((double)reachableInterface/interfacesCount)*((double)reachableEntities/entitiesCount+(double)reachableEntities/accumulatedDistance);

        return MIF;
    }

    public Boolean traversableOperation2Microservice(MIF1.Method operation, MIF1.Microservice microservice2, int distanceTable[]){
        ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
        if (interInvokerMethods.size()==0) return false; //当前操作未被微服务2中的实体访问

        int distance = 1 ;
        Boolean traversable = false;
        for(int i=0; i<interInvokerMethods.size(); i++){
            if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                traversable = true;
                String className = interInvokerMethods.get(i).className;
                int invokingClassIndex = microservice2.getClass2IndexMap().get(className);
                // 更新当前实体类到微服务1接口的最小距离表
                if (distanceTable[invokingClassIndex]==-1 || distanceTable[invokingClassIndex] > distance)
                    distanceTable[invokingClassIndex] = distance;

                //获得调用当前方法的方法
                String methodName = interInvokerMethods.get(i).methodName;
                MIF1.Class invokingClass = microservice2.getClassMap().get(className);
                MIF1.Method invokingMethod = invokingClass.methodMap.get(methodName);
                putIntraInvokerEntities(distance,invokingMethod,microservice2,distanceTable);
            }
        }
        return traversable;
    }

    public void putIntraInvokerEntities(int distance, MIF1.Method method, MIF1.Microservice microservice2, int distanceTable[]){
        //获得调用调用方法的方法列表
        ArrayList<IntraMicroserviceInvoker> intraInvokerMethods = method.getIntraInvokerMethods();
        if (intraInvokerMethods.size() == 0) return;
        distance++;
        for(int j=0; j < intraInvokerMethods.size(); j++){
            String className = intraInvokerMethods.get(j).className;
            int invokingClassIndex = microservice2.getClass2IndexMap().get(className);
            // 更新当前实体类到微服务1接口的最小距离表
            if (distanceTable[invokingClassIndex]==-1 || distanceTable[invokingClassIndex] > distance)
                distanceTable[invokingClassIndex] = distance;

            //获得调用方法
            MIF1.Class invokingClass = microservice2.getClassMap().get(className);
            String methodName = intraInvokerMethods.get(j).methodName;
            MIF1.Method invokingMethod = invokingClass.methodMap.get(methodName);
            putIntraInvokerEntities(distance, invokingMethod, microservice2, distanceTable);
        }
    }

    public void readASingleMicroservice(MIF1.Microservice microservice, String fileName){

        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MIF1.Method method = new MIF1.Method(list.get(1),list.get(2),list.get(3),list.get(4));
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
            MIF1.Method method = new MIF1.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);
            microservice.addMethod2Class(interfaceName,method);//把接口当作类处理
        }
    }

    public void handleClassMethodRelationship(MIF1.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            MIF1.Class class1=microservice.getClassMap().get(list.get(0));
            MIF1.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleInterface2ClassRelationship(MIF1.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,5);
        for(List<String> list:relationshipList){
            //MIF.Interface interface1=microservice.getInterfaceMap().get(list.get(0));
            MIF1.Class class1=microservice.getClassMap().get(list.get(0));//把接口当作类处理
            MIF1.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的接口到类方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleClass2InterfaceRelationship(MIF1.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,7);
        for(List<String> list:relationshipList){
            MIF1.Class class1=microservice.getClassMap().get(list.get(0));
            //MIF.Interface interface2=microservice.getInterfaceMap().get(list.get(2));
            MIF1.Class class2=microservice.getClassMap().get(list.get(2));

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
            MIF1.Microservice microservice1 = microserviceMap.get(list.get(0));
            MIF1.Microservice microservice2 = microserviceMap.get(list.get(3));

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
        Map<String, MIF1.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index; //包含普通类和接口

        Map<String, MIF1.Interface> interfaceMap;
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

        public Map<String, MIF1.Class> getClassMap() {
            return classMap;
        }

        public Map<String, MIF1.Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public Map<String, Integer> getClass2IndexMap() {
            return class2IndexMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new MIF1.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, MIF1.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new MIF1.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, MIF1.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }

        public int getOperationsCount(){
            int count = 0;
            for (MIF1.Interface interface1: this.interfaceMap.values()){
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
        Map<String, MIF1.Method> methodMap =new HashMap<>();


        public Class(String name) {
            this.name = name;
            this.publicUnitCount = 0;
            this.protectedUnitCount = 0;
            this.privateUnitCount = 0;
        }


        public void addMethod(MIF1.Method a) {
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
        Map<String, MIF1.Method> methodMap =new HashMap<>();
        int publicMethodCount;
        int protectedMethodCount;

        public Interface(String name){
            this.name = name;
            this.publicMethodCount = 0;
            this.protectedMethodCount = 0;
        }

        public void addMethod(MIF1.Method m){
            methodMap.put(m.name,m);
            if(m.modifier.equals("public"))
                publicMethodCount++;
            if(m.modifier.equals("protected"))
                protectedMethodCount++;
        }

        public int getOperations(){
            return this.protectedMethodCount + this.publicMethodCount;
        }

        public Map<String, MIF1.Method> getMethodMap() {
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
