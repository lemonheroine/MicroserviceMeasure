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

public class DPM {

    Map<String, DPM.Microservice> microserviceMap=new HashMap<>();
    Map<String,Integer> microservice2IndexMap=new HashMap<>();
    Map<Integer,String> index2MicroserviceMap=new HashMap<>();
    int microserviceIndex = 0;
    int entitiesIndex = 0;
    int interRelationIndex = 0;
    int interfaceIndex = 0;

    HashSet<ConnectivityPattern>  distinctDPTwoMicroservices;
    HashSet<Double> distinctCVMCI;
    HashSet<Double> distinctCVCaT;
    HashSet<Double> distinctCVCeT;
    HashSet<Double> distinctCVACT;

    HashSet<ConnectivityPattern> distinctDPAfferent;
    HashSet<Double> distinctCVCa;
    HashSet<Double> distinctCVaMCI;
    HashSet<Double> distinctCVAIS;

    HashSet<ConnectivityPattern> distinctDPEfferent;
    HashSet<Double> distinctCVCe;
    HashSet<Double> distinctCVeMCI;
    HashSet<Double> distinctCVADS;

    int overallReachableInterface;
    int overallReachableEntities;
    int overallDistance;
    int [] reachableInterface;
    int [] overallDistanceTable;


    public static void main(String[] args) throws IOException {

        runningDPMCalculation();

    }

    public static void runningDPMCalculation() throws IOException {
        String microservicesRecordFile = "svc_info.txt";
        String projectPrefix = "data/structure_data_0612/";
        String projectsListFile = "projectsList.txt";
        BufferedReader br = new BufferedReader(new FileReader(projectPrefix+projectsListFile));
        String projectLocation = "";
        Workbook workbook  = new XSSFWorkbook(); //创建用于写入结果的excel工具类
        Sheet sheet = Util.createDPMResult(workbook,"DPMResult");
        int rowIndex = 0;
        while ((projectLocation=br.readLine())!=null){

            String microservicesDependencies = "/"+projectLocation+"_structure.xlsx";
            List<String> microservicesList = Arrays.asList(new String(Files.readAllBytes(Paths.get(projectPrefix+projectLocation+"/"+microservicesRecordFile)), StandardCharsets.UTF_8).split(",", -1));

            DPM mif = new DPM();
            mif.calculateMicroservicesMIF(workbook,projectPrefix+projectLocation+"/",microservicesList,microservicesDependencies);
            mif.calculateAfferentMIF();
            mif.calculateEfferentMIF();
            mif.printProjectDPM(sheet,rowIndex++,projectLocation);

        }
        Util.writeExcel(workbook,projectPrefix+"DPMResult.xlsx");

    }

    public void printProjectDPM(Sheet sheet, int rowIndex,String projectName){
        Util.writeToDPMResult(sheet,rowIndex,projectName,microserviceIndex,
                this.distinctDPTwoMicroservices.size(),this.distinctCVMCI.size(),this.distinctCVCaT.size(),this.distinctCVCeT.size(),this.distinctCVACT.size(),
                this.distinctDPAfferent.size(),this.distinctCVaMCI.size(),this.distinctCVCa.size(),this.distinctCVAIS.size(),
                this.distinctDPEfferent.size(),this.distinctCVeMCI.size(),this.distinctCVCe.size(),this.distinctCVADS.size());

    }

    private void calculateAfferentMIF(){
        for(DPM.Microservice microservice1: microserviceMap.values()){
            int interfaceCount = microservice1.interfaceIndex;
            int allEntitiesCount = 0;
            this.overallReachableEntities = 0;
            this.overallDistance = 0;
            this.overallReachableInterface = 0;
            this.reachableInterface = new int[interfaceCount];
            for(int i=0;i<interfaceCount;i++)
                this.reachableInterface[i] =0;
            for(DPM.Microservice microservice2: microserviceMap.values()){
                if (microservice1.equals(microservice2))continue;
                int entitiesCount = microservice2.index;
                allEntitiesCount += entitiesCount;
                calculateTwoMicroservicesMIF(1,microservice1, microservice2);
            }

            for(int i=0;i<interfaceCount;i++){
                this.overallReachableInterface += this.reachableInterface[i];
            }
            if(this.overallReachableInterface==0||this.overallReachableEntities==0){
                microservice1.MIFa = 0;
            }else microservice1.MIFa =  ((double)this.overallReachableInterface/interfaceCount) *
                    ((double)this.overallReachableEntities/allEntitiesCount + (double)this.overallReachableEntities/this.overallDistance);
        }
    }

    private void calculateEfferentMIF(){
        for(DPM.Microservice microservice1: microserviceMap.values()){
            int entitiesCount = microservice1.index;
            int allInterfacesCount = 0;
            this.overallReachableEntities = 0;
            this.overallDistance = 0;
            this.overallReachableInterface = 0;
            this.overallDistanceTable = new int[entitiesCount];
            for(int i=0;i<entitiesCount;i++)
                this.overallDistanceTable[i] = -1;

            for(DPM.Microservice microservice2: microserviceMap.values()){
                if (microservice1.equals(microservice2))continue;
                int interfacesCount = microservice2.interfaceIndex;
                allInterfacesCount += interfacesCount;
                calculateTwoMicroservicesMIF(2,microservice2, microservice1);
            }

            for(int i=0;i<entitiesCount;i++){
                if(this.overallDistanceTable[i] != -1) {
                    this.overallReachableEntities += 1;
                    this.overallDistance += this.overallDistanceTable[i];
                }
            }

            if(this.overallReachableInterface==0||this.overallReachableEntities==0){
                microservice1.MIFe = 0;
            }else microservice1.MIFe = ((double)this.overallReachableInterface/allInterfacesCount) *
                    ((double)this.overallReachableEntities/entitiesCount + (double)this.overallReachableEntities/this.overallDistance);

        }
    }


    public void calculateMicroservicesMIF(Workbook workbook,String projectPath, List<String>  microservicesList,String microservicesDependencies){
        //先读取每个微服务结构数据
        for (String microserviceName: microservicesList){
            if (!microserviceMap.containsKey(microserviceName)){
                DPM.Microservice microservice = new DPM.Microservice(microserviceName);
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
        double[][] ACT2MicroservicesMatrix =new double[microserviceIndex][microserviceIndex]; //记录AIS和ADS基础指标的矩阵
        int[][] CITwoMicroservicesMatrix = new int[microserviceIndex][microserviceIndex];

        this.distinctDPTwoMicroservices = new HashSet<>();
        this.distinctCVMCI = new HashSet<>();
        this.distinctCVCaT = new HashSet<>();
        this.distinctCVCeT = new HashSet<>();
        this.distinctCVACT = new HashSet<>();

        this.distinctDPAfferent = new HashSet<>();
        this.distinctCVCa = new HashSet<>();
        this.distinctCVaMCI = new HashSet<>();
        this.distinctCVAIS = new HashSet<>();

        this.distinctDPEfferent = new HashSet<>();
        this.distinctCVCe = new HashSet<>();
        this.distinctCVeMCI = new HashSet<>();
        this.distinctCVADS = new HashSet<>();


        for(DPM.Microservice microservice1: microserviceMap.values()){
            double MIFaMicroservice = 0;
            int microservice1Index = microservice2IndexMap.get(microservice1.name);
            int allEntitiesCount = 0;
            int distintCI = 0;
            int CaMicroservice = 0;
            int CeMicroservice = 0;
            int AISMicroservice = 0;
            int ADSMicroservice = 0;

            for(DPM.Microservice microservice2: microserviceMap.values()){
                // 计算MIF指标
                if (microservice1.equals(microservice2))continue;
                int entitiesCount = microservice2.index;
                allEntitiesCount += entitiesCount;
                int microservice2Index = microservice2IndexMap.get(microservice2.name);

                int CI = calculateCITwoMicroservice(microservice1,microservice2);
                distintCI += CI;
                CITwoMicroservicesMatrix[microservice1Index][microservice2Index] = CI;
                this.distinctDPTwoMicroservices.add(new ConnectivityPattern(microservice1.interfaceIndex,entitiesCount,CI));

                double MIF2Microservices = calculateTwoMicroservicesMIF(0,microservice1, microservice2);
                this.distinctCVMCI.add(MIF2Microservices);
                MIFaMicroservice += MIF2Microservices;


                // to what extent microservice 2 depends on microservice 1
                MIF2MicroservicesMatrix[microservice1Index][microservice2Index] = MIF2Microservices;

                // 计算Ca指标: how many classes in microservice 2 depend on microservice 1
                double CaT = calculateTwoMicroservicesCa(microservice1, microservice2);
                this.distinctCVCaT.add(CaT);
                Ca2MicroservicesMatrix[microservice1Index][microservice2Index] = CaT;
                CaMicroservice += Ca2MicroservicesMatrix[microservice1Index][microservice2Index];
                // 计算Ce指标: how many interfaces of microservice2 are depended upon by microservice1
                double CeT = calculateTwoMicroservicesCe(microservice1, microservice2);
                this.distinctCVCeT.add(CeT);
                Ce2MicroservicesMatrix[microservice1Index][microservice2Index] = CeT;
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

                this.distinctCVACT.add(ACT2MicroservicesMatrix[microservice1Index][microservice2Index]);

            }

            microservice1.AIS = AISMicroservice;
            this.distinctCVAIS.add(microservice1.AIS);

            microservice1.ADS = ADSMicroservice;
            this.distinctCVADS.add(microservice1.ADS);

            microservice1.Ca = CaMicroservice;
            this.distinctCVCa.add(microservice1.Ca);

            microservice1.Ce = CeMicroservice;
            this.distinctCVCe.add(microservice1.Ce);

            this.distinctDPAfferent.add(new ConnectivityPattern(microservice1.interfaceIndex,allEntitiesCount,distintCI));

//            microservice1.MIFa = (double)MIFaMicroservice/(microserviceIndex-1);
            microservice1.MIFa = (double)MIFaMicroservice/(microserviceIndex-1);
            this.distinctCVaMCI.add(microservice1.MIFa);

            entitiesIndex += microservice1.index;
            interfaceIndex += microservice1.interfaceIndex;
        }

        //计算传入依赖带来的微服务间影响MIF
        for (int i =0;i<microserviceIndex;i++){
            double MIFeMicroservice = 0;
            int allInterfacesCount = 0;
            int distinctDI = 0;

            for (int j=0;j<microserviceIndex;j++){
                if (j==i)continue;
                int curInterfacesCount = microserviceMap.get(index2MicroserviceMap.get(j)).interfaceIndex;
                allInterfacesCount += curInterfacesCount;
                distinctDI += CITwoMicroservicesMatrix[j][i];
                MIFeMicroservice += MIF2MicroservicesMatrix[j][i];

            }
            this.distinctDPEfferent.add(new ConnectivityPattern(allInterfacesCount,microserviceMap.get(index2MicroserviceMap.get(i)).index,distinctDI));
            MIFeMicroservice /= (microserviceIndex-1);
            microserviceMap.get(index2MicroserviceMap.get(i)).MIFe = MIFeMicroservice;
            this.distinctCVeMCI.add(MIFeMicroservice);

        }

        System.out.println("Microservices count of this system is: "+ microserviceIndex);
        System.out.println("Entities count of this system is: "+ entitiesIndex);
        System.out.println("Interfaces count of this system is: "+ interfaceIndex);
        System.out.println("InterRelationIndex count of this system is: "+ interRelationIndex);
    }

    // calculating how many classes in microservice2 depend on microservice1
    public int calculateTwoMicroservicesCa(DPM.Microservice microservice1, DPM.Microservice microservice2){

        HashSet<DPM.Class> reachableEntities = new HashSet<>();
        for (DPM.Interface interface1: microservice1.getInterfaceMap().values()){
            for (DPM.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();
                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                        String className = interInvokerMethods.get(i).className;
                        DPM.Class invokingClass = microservice2.getClassMap().get(className);
                        reachableEntities.add(invokingClass);// 将遍历到的类放入set
                    }
                }
            }
        }
        return reachableEntities.size();
    }

    public int calculateCITwoMicroservice(DPM.Microservice microservice1, DPM.Microservice microservice2){
        HashSet<CouplingInteraction> ci = new HashSet<>();

        for (DPM.Interface interface1: microservice1.getInterfaceMap().values()){
            for (DPM.Method operation: interface1.getMethodMap().values()){
                ArrayList<InterMicroserviceInvoker> interInvokerMethods = operation.getInterInvokerMethods();

                for(int i=0; i<interInvokerMethods.size(); i++){
                    if (interInvokerMethods.get(i).microserviceName.equals(microservice2.name)) {// 仅处理与当前微服务相关的实体方法
                        String className = interInvokerMethods.get(i).className;
                        DPM.Class invokingClass = microservice2.getClassMap().get(className);
                        ci.add(new CouplingInteraction(invokingClass,interface1));
                    }
                }
            }
        }
        return ci.size();
    }

    public int calculateTwoMicroservicesCe(DPM.Microservice microservice1, DPM.Microservice microservice2){
        // calculating how many interfaces of microservice2 are depended upon by microservice1
        HashSet<DPM.Interface> reachableInterfaces = new HashSet<>();
        for (DPM.Interface interface1: microservice2.getInterfaceMap().values()){
            for (DPM.Method operation: interface1.getMethodMap().values()){
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
    public double calculateTwoMicroservicesMIF(int mode, DPM.Microservice microservice1, DPM.Microservice microservice2){
        int interfacesCount = microservice1.interfaceIndex;
        int entitiesCount = microservice2.index;
        if (interfacesCount == 0 || entitiesCount == 0) return 0;
        int distanceTable[] = new int[entitiesCount];

        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<entitiesCount;i++) distanceTable[i] = -1;
        int reachableInterface = 0;
        // 遍历微服务1的所有操作以更新可达的微服务2中实体对应的距离表
        for (DPM.Interface interface1: microservice1.getInterfaceMap().values()){
            Boolean interfaceReachableFlag = false;
            for (DPM.Method operation: interface1.getMethodMap().values()){
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

        double MIF = ((double)reachableInterface/interfacesCount)*((double)reachableEntities/entitiesCount+(double)reachableEntities/accumulatedDistance);

        return MIF;
    }

    public Boolean traversableOperation2Microservice(DPM.Method operation, DPM.Microservice microservice2, int distanceTable[]){
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
                DPM.Class invokingClass = microservice2.getClassMap().get(className);
                DPM.Method invokingMethod = invokingClass.methodMap.get(methodName);
                putIntraInvokerEntities(distance,invokingClass,invokingMethod,microservice2,distanceTable);
            }
        }
        return traversable;
    }

    public void putIntraInvokerEntities(int distance, DPM.Class currentClass, DPM.Method method, DPM.Microservice microservice2, int distanceTable[]){
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
            DPM.Class invokingClass = microservice2.getClassMap().get(className);
            String methodName = intraInvokerMethods.get(j).methodName;
            DPM.Method invokingMethod = invokingClass.methodMap.get(methodName);
            putIntraInvokerEntities(currentDistance, invokingClass,invokingMethod, microservice2, distanceTable);
        }
    }

    public void readASingleMicroservice(DPM.Microservice microservice, String fileName){

        //读取类方法表信息
        List<List<String>> classMethodList=Util.readExcel(fileName,5,0);
        for(List<String> list:classMethodList){
            if(list.get(0).equals(""))
                continue;
            String className = list.get(0);
            microservice.addClass(className);

            if(list.get(1).equals(""))
                continue;
            DPM.Method method = new DPM.Method(list.get(1),list.get(2),list.get(3),list.get(4));
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
            DPM.Method method = new DPM.Method(list.get(1),list.get(2),list.get(3),list.get(4));
            microservice.addMethod2Interface(interfaceName,method);
            microservice.addMethod2Class(interfaceName,method);//把接口当作类处理
        }
    }

    public void handleClassMethodRelationship(DPM.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,2);
        for(List<String> list:relationshipList){
            DPM.Class class1=microservice.getClassMap().get(list.get(0));
            DPM.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleInterface2ClassRelationship(DPM.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,5);
        for(List<String> list:relationshipList){
            //MIF.Interface interface1=microservice.getInterfaceMap().get(list.get(0));
            DPM.Class class1=microservice.getClassMap().get(list.get(0));//把接口当作类处理
            DPM.Class class2=microservice.getClassMap().get(list.get(2));

            if(class1==null||class2==null)
                continue;

            //将每个遍历到的接口到类方法间调用关系记到被调用方法的内部list中
            microservice.getClassMap().get(list.get(2)).putMethodInvoker(list.get(3),list.get(0),list.get(1));
        }
    }

    public void handleClass2InterfaceRelationship(DPM.Microservice microservice, String fileName){
        List<List<String>> relationshipList = Util.readExcel(fileName,5,7);
        for(List<String> list:relationshipList){
            DPM.Class class1=microservice.getClassMap().get(list.get(0));
            //MIF.Interface interface2=microservice.getInterfaceMap().get(list.get(2));
            DPM.Class class2=microservice.getClassMap().get(list.get(2));

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
            DPM.Microservice microservice1 = microserviceMap.get(list.get(0));
            DPM.Microservice microservice2 = microserviceMap.get(list.get(3));

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
        Map<String, DPM.Class> classMap;
        Map<String,Integer> class2IndexMap;
        Map<Integer,String> index2ClassMap;
        int index; //包含普通类和接口

        Map<String, DPM.Interface> interfaceMap;
        Map<String,Integer> interface2IndexMap;
        Map<Integer,String> index2InterfaceMap;
        int interfaceIndex;

        double MIFa, MIFe, Ca, Ce, AIS, ADS, MIFetestB,MIFetestb;

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

        public Map<String, DPM.Class> getClassMap() {
            return classMap;
        }

        public Map<String, DPM.Interface> getInterfaceMap() {
            return interfaceMap;
        }

        public Map<String, Integer> getClass2IndexMap() {
            return class2IndexMap;
        }

        public void addClass(String className){
            if(!classMap.containsKey(className)){
                classMap.put(className,new DPM.Class(className));
                class2IndexMap.put(className,index);
                index2ClassMap.put(index++,className);
            }
        }

        public void addMethod2Class(String className, DPM.Method method){
            classMap.get(className).addMethod(method);
        }

        public void addInterface(String interfaceName){
            if(!interfaceMap.containsKey(interfaceName)){
                interfaceMap.put(interfaceName,new DPM.Interface(interfaceName));
                interface2IndexMap.put(interfaceName,interfaceIndex);
                index2InterfaceMap.put(interfaceIndex++,interfaceName);
            }
        }

        public void addMethod2Interface(String interfaceName, DPM.Method method){
            interfaceMap.get(interfaceName).addMethod(method);
        }

        public int getOperationsCount(){
            int count = 0;
            for (DPM.Interface interface1: this.interfaceMap.values()){
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
        Map<String, DPM.Method> methodMap =new HashMap<>();


        public Class(String name) {
            this.name = name;
            this.publicUnitCount = 0;
            this.protectedUnitCount = 0;
            this.privateUnitCount = 0;
        }


        public void addMethod(DPM.Method a) {
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
        Map<String, DPM.Method> methodMap =new HashMap<>();
        int publicMethodCount;
        int protectedMethodCount;

        public Interface(String name){
            this.name = name;
            this.publicMethodCount = 0;
            this.protectedMethodCount = 0;
        }

        public void addMethod(DPM.Method m){
            methodMap.put(m.name,m);
            if(m.modifier.equals("public"))
                publicMethodCount++;
            if(m.modifier.equals("protected"))
                protectedMethodCount++;
        }

        public int getOperations(){
            return this.protectedMethodCount + this.publicMethodCount;
        }

        public Map<String, DPM.Method> getMethodMap() {
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

    class ConnectivityPattern{
        int interfaceCount;
        int entityCount;
        int couplingInteraction;

        public ConnectivityPattern(int interfaceCount,int entityCount,int couplingInteraction){
            this.interfaceCount = interfaceCount;
            this.entityCount = entityCount;
            this.couplingInteraction = couplingInteraction;
        }

        @Override
        public int hashCode(){
            return this.interfaceCount+this.entityCount+this.couplingInteraction;
        }

        @Override
        public boolean equals(Object obj){
            return this.interfaceCount == (((ConnectivityPattern)obj).interfaceCount)
                    && this.entityCount == (((ConnectivityPattern)obj).entityCount)
                    && this.couplingInteraction == (((ConnectivityPattern)obj).couplingInteraction);
        }
    }

    class CouplingInteraction{
        DPM.Class invokingClass;
        DPM.Interface invokedInterface;

        public CouplingInteraction(DPM.Class invokingClass, DPM.Interface invokedInterface){
            this.invokingClass = invokingClass;
            this.invokedInterface = invokedInterface;
        }

        @Override
        public int hashCode(){
            return this.invokingClass.hashCode()+this.invokedInterface.hashCode();
        }

        @Override
        public boolean equals(Object obj){
            return this.invokingClass.equals(((CouplingInteraction)obj).invokingClass)
                    && this.invokedInterface.equals(((CouplingInteraction)obj).invokedInterface);
        }

    }

}
