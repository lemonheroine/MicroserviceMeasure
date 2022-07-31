import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MCI {

    Map<String, MCI.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;
    int entitiesIndex = 0;
    int interRelationIndex = 0;
    int interfaceIndex = 0;
    int overallReachableInterface;
    int overallReachableEntities;
    int overallDistance;
    int [] reachableInterface;
    int [] overallDistanceTable;


    public static void main(String[] args) throws IOException {

        runningBasicData();

    }

    public static void runningBasicData() throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data/structure_data_0612/";
        String projectsListFile = "projectsList.txt";
        BufferedReader br = new BufferedReader(new FileReader(projectPrefix+projectsListFile));
        String projectLocation = "";
        while ((projectLocation=br.readLine())!=null){
            Workbook workbook  = new XSSFWorkbook(); //创建用于写入结果的excel工具类
            String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
            List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

            MCI mci = new MCI();
            mci.calculateMicroservicesMCI(workbook,projectPrefix+projectLocation+"/",microservicesList,microservicesDependencies);
            mci.calculateAfferentMCI();
            mci.calculateEfferentMCI();
            mci.printMicroservicesCoupling(workbook,microservicesList);

            Util.writeExcel(workbook,projectPrefix+projectLocation+".xlsx");

        }
    }

    public static void runningOREData() throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data_0624_ORE/";

        String projectsListFile = "versionsList.txt";
        BufferedReader br = new BufferedReader(new FileReader(projectPrefix+projectsListFile));
        String projectLocation = "";
        while ((projectLocation=br.readLine())!=null){
            Workbook workbook  = new XSSFWorkbook(); //创建用于写入结果的excel工具类
            String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
            List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

            MCI mci = new MCI();
            mci.calculateMicroservicesMCI(workbook,projectPrefix+projectLocation+"/",microservicesList,microservicesDependencies);
            mci.printMicroservicesCoupling(workbook,microservicesList);

            Util.writeExcel(workbook,projectPrefix+projectLocation+".xlsx");

        }
    }

    public void printMicroservicesCoupling(Workbook workbook,List<String> microservicesList){
        Sheet sheet = Util.createSheet2(workbook);
        int rowIndex = 0;

        for (String micro: microservicesList){
            Util.writeMicroserviceToSheet2(sheet, rowIndex++,microserviceMap.get(micro));
        }
    }

    public void print2MicroservicesCoupling(Workbook workbook,double[][] MCI2MicroservicesMatrix,double[][] Ca2MicroservicesMatrix,
                                            double[][] Ce2MicroservicesMatrix,double[][] ACT2MicroservicesMatrix){
        Sheet sheet = Util.createSheet1(workbook);
        int rowIndex = 0;

        for(int i=0;i<microserviceIndex;i++){
            for(int j=0;j<microserviceIndex;j++){
                if (i==j) continue;
                String dstMicroservice = index2MicroserviceMap.get(i);
                String orgMicroservice = index2MicroserviceMap.get(j);
                int EntitiesOfOrg = microserviceMap.get(orgMicroservice).getEntities();
                int InterfaceOfOrg = microserviceMap.get(orgMicroservice).interfaceIndex;
                int EntitiesOfDst = microserviceMap.get(dstMicroservice).getEntities();
                int InterfaceOfDst = microserviceMap.get(dstMicroservice).interfaceIndex;


                double MCI = MCI2MicroservicesMatrix[i][j]; // j depends on i
                double Ca = Ca2MicroservicesMatrix[i][j];
                double Ce = Ce2MicroservicesMatrix[j][i];
                double ACT = ACT2MicroservicesMatrix[i][j];
                Util.writeMicroserviceToSheet1(sheet, rowIndex++,orgMicroservice,EntitiesOfOrg, InterfaceOfOrg,
                        dstMicroservice,EntitiesOfDst,InterfaceOfDst,
                        ACT,Ca, Ce, MCI);
            }
        }
    }


    private void calculateAfferentMCI(){
        for(MCI.Microservice microservice1: microserviceMap.values()){
            int interfaceCount = microservice1.interfaceIndex;
            int allEntitiesCount = 0;
            this.overallReachableEntities = 0;
            this.overallDistance = 0;
            this.overallReachableInterface = 0;
            this.reachableInterface = new int[interfaceCount];
            for(int i=0;i<interfaceCount;i++)
                this.reachableInterface[i] =0;
            for(MCI.Microservice microservice2: microserviceMap.values()){
                if (microservice1.equals(microservice2))continue;
                int entitiesCount = microservice2.index;
                allEntitiesCount += entitiesCount;
                calculateTwoMicroservicesMCI(1,microservice1, microservice2);
            }

            for(int i=0;i<interfaceCount;i++){
                this.overallReachableInterface += this.reachableInterface[i];
            }
            if(this.overallReachableInterface==0||this.overallReachableEntities==0){
                microservice1.MCIa = 0;
            }else microservice1.MCIa =  ((double)this.overallReachableInterface/interfaceCount) *
                    ((double)this.overallReachableEntities/allEntitiesCount + (double)this.overallReachableEntities/this.overallDistance);
        }
    }

    private void calculateEfferentMCI(){
        for(MCI.Microservice microservice1: microserviceMap.values()){
            int entitiesCount = microservice1.index;
            int allInterfacesCount = 0;
            this.overallReachableEntities = 0;
            this.overallDistance = 0;
            this.overallReachableInterface = 0;
            this.overallDistanceTable = new int[entitiesCount];
            for(int i=0;i<entitiesCount;i++)
                this.overallDistanceTable[i] = -1;

            for(MCI.Microservice microservice2: microserviceMap.values()){
                if (microservice1.equals(microservice2))continue;
                int interfacesCount = microservice2.interfaceIndex;
                allInterfacesCount += interfacesCount;
                calculateTwoMicroservicesMCI(2,microservice2, microservice1);
            }

            for(int i=0;i<entitiesCount;i++){
                if(this.overallDistanceTable[i] != -1) {
                    this.overallReachableEntities += 1;
                    this.overallDistance += this.overallDistanceTable[i];
                }
            }

            if(this.overallReachableInterface==0||this.overallReachableEntities==0){
                microservice1.MCIe = 0;
            }else microservice1.MCIe = ((double)this.overallReachableInterface/allInterfacesCount) *
                    ((double)this.overallReachableEntities/entitiesCount + (double)this.overallReachableEntities/this.overallDistance);

        }
    }

    public void calculateMicroservicesMCI(Workbook workbook,String projectPath, List<String>  microservicesList,String microservicesDependencies){
        //先读取每个微服务结构数据
        for (String microserviceName: microservicesList){
            if (!microserviceMap.containsKey(microserviceName)){
                MCI.Microservice microservice = new MCI.Microservice(microserviceName);
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

        double[][] MCI2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录MCI指标的矩阵
        double[][] Ca2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ca基础指标的矩阵
        double[][] Ce2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; // 记录Ce基础指标的矩阵
        double[][] ACT2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; //记录AIS和ADS基础指标的矩阵

        for(MCI.Microservice microservice1: microserviceMap.values()){
            int microservice1Index = microservice2IndexMap.get(microservice1.name);
            int allEntitiesCount = 0;
            int CaMicroservice = 0;
            int CeMicroservice = 0;
            int AISMicroservice = 0;
            int ADSMicroservice = 0;

            for(MCI.Microservice microservice2: microserviceMap.values()){
                // 计算MCI指标
                if (microservice1.equals(microservice2))continue;
                int entitiesCount = microservice2.index;
                allEntitiesCount += entitiesCount;
                double MCI2Microservices = calculateTwoMicroservicesMCI(0, microservice1, microservice2);


                int microservice2Index = microservice2IndexMap.get(microservice2.name);
                // to what extent microservice 2 depends on microservice 1
                MCI2MicroservicesMatrix[microservice1Index][microservice2Index] = MCI2Microservices;

                // 计算Ca指标: how many classes in microservice 2 depend on microservice 1
                Ca2MicroservicesMatrix[microservice1Index][microservice2Index] = calculateTwoMicroservicesCa(microservice1, microservice2);
                CaMicroservice += Ca2MicroservicesMatrix[microservice1Index][microservice2Index];
                // 计算Ce指标: how many interfaces of microservice2 are depended upon by microservice1
                Ce2MicroservicesMatrix[microservice1Index][microservice2Index] = calculateTwoMicroservicesCe(microservice1, microservice2);
                CeMicroservice += Ce2MicroservicesMatrix[microservice1Index][microservice2Index];

                //根据Ca矩阵计算AIS
                if (Ca2MicroservicesMatrix[microservice1Index][microservice2Index]!=0){
                    ACT2MicroservicesMatrix[microservice1Index][microservice2Index] = 1; // microservice 2 depend on microservice 1
                    AISMicroservice++;
                }
                //根据Ce矩阵计算ADS
                if (Ce2MicroservicesMatrix[microservice1Index][microservice2Index]!=0){
                    ADSMicroservice++;
                }

            }

            microservice1.AIS = AISMicroservice;
            microservice1.ADS = ADSMicroservice;
            microservice1.Ca = CaMicroservice;
            microservice1.Ce = CeMicroservice;

            entitiesIndex += microservice1.index;
            interfaceIndex += microservice1.interfaceIndex;
        }

        print2MicroservicesCoupling(workbook,MCI2MicroservicesMatrix,Ca2MicroservicesMatrix,
                Ce2MicroservicesMatrix,ACT2MicroservicesMatrix);

        System.out.println("Microservices count of this system is: "+ microserviceIndex);
        System.out.println("Entities count of this system is: "+ entitiesIndex);
        System.out.println("Interfaces count of this system is: "+ interfaceIndex);
        System.out.println("InterRelationIndex count of this system is: "+ interRelationIndex);
    }

    // calculating how many classes in microservice2 depend on microservice1
    public int calculateTwoMicroservicesCa(MCI.Microservice microservice1, MCI.Microservice microservice2){

        HashSet<MCI.Class> reachableEntities = new HashSet<>();
        for (MCI.Interface interface1: microservice1.getInterfaceMap().values()){
            for (MCI.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                        String className = interInvokerMethods.get(i).className;
                        MCI.Class invokingClass = microservice2.getClassMap().get(className);
                        reachableEntities.add(invokingClass);// 将遍历到的类放入set
                    }
                }
            }
        }
        return reachableEntities.size();
    }

    public int calculateTwoMicroservicesCe(MCI.Microservice microservice1, MCI.Microservice microservice2){
        // calculating how many interfaces of microservice2 are depended upon by microservice1
        HashSet<MCI.Interface> reachableInterfaces = new HashSet<>();
        for (MCI.Interface interface1: microservice2.getInterfaceMap().values()){
            for (MCI.Method operation: interface1.getMethodMap().values()){
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

    // calculating to what extent microservice 2 depends on microservice 1
    public double calculateTwoMicroservicesMCI(int mode, MCI.Microservice microservice1, MCI.Microservice microservice2){
        int interfacesCount = microservice1.interfaceIndex;
        int entitiesCount = microservice2.index;
        if (interfacesCount == 0 || entitiesCount == 0) return 0;
        int distanceTable[] = new int[entitiesCount];

        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<entitiesCount;i++) distanceTable[i] = -1;
        int reachableInterface = 0;
        // 遍历微服务1的所有操作以更新可达的微服务2中实体对应的距离表
        for (MCI.Interface interface1: microservice1.getInterfaceMap().values()){
            Boolean interfaceReachableFlag = false;
            for (MCI.Method operation: interface1.getMethodMap().values()){
                // 计算所有微服务2中实体到当前操作的距离并更新distance表
                if (traversableOperation2Microservice(operation,microservice2,distanceTable)) interfaceReachableFlag = true;
            }
            if (interfaceReachableFlag) {
                reachableInterface++; //被遍历过的此接口对微服务2而言是可遍历到的
                if(mode==1){
                    int interfaceIndex = microservice1.interface2IndexMap.get(interface1.name);
                    this.reachableInterface[interfaceIndex] = 1;
                }
            }
        }
        if (reachableInterface==0) return 0;
        if(mode==2){
            this.overallReachableInterface += reachableInterface;
        }

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        for(int i =0; i<entitiesCount;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];

                if(mode==2){
                    if(distanceTable[i]<this.overallDistanceTable[i]||this.overallDistanceTable[i]==-1)this.overallDistanceTable[i] = distanceTable[i];
                }
            }
        }
        if (reachableEntities == 0) return 0;

        if(mode==1){
            this.overallReachableEntities += reachableEntities;
            this.overallDistance += accumulatedDistance;
        }

        double MCI = ((double)reachableInterface/interfacesCount)*((double)reachableEntities/entitiesCount+(double)reachableEntities/accumulatedDistance);

        return MCI;
    }

    public Boolean traversableOperation2Microservice(MCI.Method operation, MCI.Microservice microservice2, int distanceTable[]){
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
                MCI.Class invokingClass = microservice2.getClassMap().get(className);
                MCI.Method invokingMethod = invokingClass.methodMap.get(methodName);
                putIntraInvokerEntities(distance,invokingClass,invokingMethod,microservice2,distanceTable);
            }
        }
        return traversable;
    }

    public void putIntraInvokerEntities(int distance, MCI.Class currentClass, MCI.Method method, MCI.Microservice microservice2, int distanceTable[]){
        //获得调用调用方法的方法列表
        ArrayList<IntraMicroserviceInvoker> intraInvokerMethods = method.getIntraInvokerMethods();
        if (intraInvokerMethods.size() == 0) return;
        distance++;
        for(int j=0; j < intraInvokerMethods.size(); j++){
            String className = intraInvokerMethods.get(j).className;
            int invokingClassIndex = microservice2.getClass2IndexMap().get(className);
            int currentDistance = distance;
            if(!currentClass.name.equals(className)){
                currentDistance -= 1;
            }
            // 更新当前实体类到微服务1接口的最小距离表
            if (distanceTable[invokingClassIndex]==-1 || distanceTable[invokingClassIndex] > currentDistance)
                 distanceTable[invokingClassIndex] = currentDistance;

            //获得调用方法
            MCI.Class invokingClass = microservice2.getClassMap().get(className);
            String methodName = intraInvokerMethods.get(j).methodName;
            MCI.Method invokingMethod = invokingClass.methodMap.get(methodName);
            putIntraInvokerEntities(currentDistance, invokingClass,invokingMethod, microservice2, distanceTable);
        }
    }

    public void readASingleMicroservice(MCI.Microservice microservice, String fileName){

        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            MCI.Method method = new MCI.Method(list.get(1),list.get(2),list.get(3),list.get(4));
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
            MCI.Method method = new MCI.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);
            microservice.addMethod2Class(interfaceName,method);//把接口当作类处理
        }
    }

    public void handleClassMethodRelationship(MCI.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            MCI.Class class1=microservice.getClassMap().get(list.get(0));
            MCI.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleInterface2ClassRelationship(MCI.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,5);
        for(List<String> list:relationshipList){
            MCI.Class class1=microservice.getClassMap().get(list.get(0));//把接口当作类处理
            MCI.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的接口到类方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleClass2InterfaceRelationship(MCI.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,7);
        for(List<String> list:relationshipList){
            MCI.Class class1=microservice.getClassMap().get(list.get(0));
            MCI.Class class2=microservice.getClassMap().get(list.get(2));

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
            MCI.Microservice microservice1 = microserviceMap.get(list.get(0));
            MCI.Microservice microservice2 = microserviceMap.get(list.get(3));

            if (microservice1==null||microservice2==null)
                continue;

            int index1= microservice2IndexMap.get(list.get(0));
            int index2= microservice2IndexMap.get(list.get(3));

            // 判断是否是来自同一个微服务的调用
            if (index1 == index2) System.out.println("invoking from the same microservices!!!");
            else interRelationIndex++;
//            System.out.println(list);
            //将每个遍历到的方法到接口操作间调用关系记到被调用操作的内部list中
            microservice2.getInterfaceMap().get(list.get(4)).putInterMethodInvoker(list.get(5),list.get(0),list.get(1),list.get(2));
        }
    }


    class Microservice{
        String name;
        Map<String, MCI.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index; //包含普通类和接口

        Map<String, MCI.Interface> interfaceMap;
        Map<String,Integer> interface2IndexMap;
        Map<Integer,String> index2InterfaceMap;
        int interfaceIndex;

        double MCIa, MCIe, Ca, Ce, AIS, ADS, MCIetestB,MCIetestb;

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

        public Map<String, MCI.Class> getClassMap() {
            return classMap;
        }

        public Map<String, MCI.Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public Map<String, Integer> getClass2IndexMap() {
            return class2IndexMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new MCI.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, MCI.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new MCI.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, MCI.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }

        public int getOperationsCount(){
            int count = 0;
            for (MCI.Interface interface1: this.interfaceMap.values()){
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
        Map<String, MCI.Method> methodMap =new HashMap<>();


        public Class(String name) {
            this.name = name;
            this.publicUnitCount = 0;
            this.protectedUnitCount = 0;
            this.privateUnitCount = 0;
        }


        public void addMethod(MCI.Method a) {
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
        Map<String, MCI.Method> methodMap =new HashMap<>();
        int publicMethodCount;
        int protectedMethodCount;

        public Interface(String name){
            this.name = name;
            this.publicMethodCount = 0;
            this.protectedMethodCount = 0;
        }

        public void addMethod(MCI.Method m){
            methodMap.put(m.name,m);
            if(m.modifier.equals("public"))
                publicMethodCount++;
            if(m.modifier.equals("protected"))
                protectedMethodCount++;
        }

        public int getOperations(){
            return this.protectedMethodCount + this.publicMethodCount;
        }

        public Map<String, MCI.Method> getMethodMap() {
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
