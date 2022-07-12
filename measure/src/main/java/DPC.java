

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class DPC {

    int microservicesCount;
    int entitiesCount;
    int interfacesCount;
    HashSet<DependenceMatrix> distinctDepMatrix;
    HashSet<Double> distinctMIFaMeasurements;
    HashSet<Double> distinctMIFeMeasurements;
    HashSet<Double> distinctMIFMeasurements;

    HashSet<Double> distinctCaaMeasurements;//区分不同微服务依赖时
    HashSet<Double> distinctCeeMeasurements;
    HashSet<Double> distinctCaMeasurements;//不区分不同微服务依赖时
    HashSet<Double> distinctCeMeasurements;

    HashSet<Double> distinctAISMeasurements;
    HashSet<Double> distinctADSMeasurements;
    HashSet<Double> distinctADMeasurements;

    Boolean matrixExistence = false;
    Boolean differentiateMicroservices; //计算模式包括是否区分不同微服务之间的依赖

    public static void main(String[] args) throws IOException {
        int []microserviceNumbers = {2};
        int []entityNumbers = {3};
        int []interfaceNumbers = {1,2,3,4,5,6,7,8,9,10};

        for(int j = 0; j<entityNumbers.length;j++){
            for(int k=0;k<interfaceNumbers.length;k++){
                DPC dpc = new DPC(entityNumbers[j],interfaceNumbers[k],true);
                for(int i = 0;i<microserviceNumbers.length;i++){
                    BufferedWriter out = new BufferedWriter(
                            new FileWriter("result_"+microserviceNumbers[i]+"_"+entityNumbers[j]+"_"+interfaceNumbers[k]+".txt"));
                    dpc.microservicesCount = microserviceNumbers[i];
                    // calculate DPC values here
                    dpc.calculateMetricsDPC(out);
                    out.close();
                }

            }
        }

        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    }

    private DPC(int entitiesCount,int interfacesCount, boolean mode){
//        this.microservicesCount = microservicesCount;
        this.entitiesCount = entitiesCount;
        this.interfacesCount = interfacesCount;
        this.differentiateMicroservices = mode;

        distinctDepMatrix = new HashSet<DependenceMatrix>();
        distinctMIFaMeasurements = new HashSet<Double>();
        distinctMIFeMeasurements = new HashSet<Double>();
        distinctMIFMeasurements = new HashSet<Double>();

        distinctCaMeasurements = new HashSet<Double>();
        distinctCeMeasurements = new HashSet<Double>();
        distinctCaaMeasurements = new HashSet<Double>();
        distinctCeeMeasurements = new HashSet<Double>();

        distinctAISMeasurements = new HashSet<Double>();
        distinctADSMeasurements = new HashSet<Double>();
        distinctADMeasurements = new HashSet<Double>();

        //在初始化时遍历所有可能的依赖模式
        DependenceMatrix curDepMatrix =new DependenceMatrix();
        tryPutMatrixIntoSet(new DependenceMatrix(curDepMatrix));
        derivePossibleMatrix(curDepMatrix,0,0);

    }

    public void calculateMetricsDPC(BufferedWriter out) throws IOException {
        String output = "";

        double distinctDepMatrixSize = distinctDepMatrix.size();
        output+= "distCP(false): "+distinctDepMatrixSize;

        if(distinctDepMatrixSize!=0){
            double DPC_MIF = (double)distinctMIFMeasurements.size()/distinctDepMatrixSize; //MIFa and MIFe
            double DPC_Ca = (double)distinctCaMeasurements.size()/distinctDepMatrixSize;
            double DPC_Ce = (double)distinctCeMeasurements.size()/distinctDepMatrixSize;
            double DPC_AD = (double)distinctADMeasurements.size()/distinctDepMatrixSize; //AIS and ADS
            output+= ", DPC_MIFa and DPC_MIFe: "+DPC_MIF + ", DPC_Ca: "+DPC_Ca+ ", DPC_Ce: "+DPC_Ce+", DPC_AIS and DPC_ADS: "+DPC_AD+"\n";

        }

        if(this.differentiateMicroservices){
            calculateDistinctMIFaMIFeWhenDifferentiate();
            calculateDistinctCaCeWhenDifferentiate();
            calculateDistinctAISADSWhenDifferentiate();
            distinctDepMatrixSize = Math.pow(distinctDepMatrixSize,(double) (microservicesCount-1)); //只计算其它n-1个服务到第n个服务之间的耦合
//            distinctDepMatrixSize = Math.pow(distinctDepMatrixSize,(double) microservicesCount*(microservicesCount-1)/2);
            if(distinctDepMatrixSize!=0){

                double DPC_MIFa = (double)distinctMIFaMeasurements.size()/distinctDepMatrixSize;
                double DPC_MIFe = (double)distinctMIFeMeasurements.size()/distinctDepMatrixSize;
                double DPC_Caa = (double)distinctCaaMeasurements.size()/distinctDepMatrixSize;
                double DPC_Cee = (double)distinctCeeMeasurements.size()/distinctDepMatrixSize;
                double DPC_AIS = (double)distinctAISMeasurements.size()/distinctDepMatrixSize;
                double DPC_ADS = (double)distinctADSMeasurements.size()/distinctDepMatrixSize;

                output+= "distCP(true): "+distinctDepMatrixSize+ ", DPC_MIFa: "+DPC_MIFa +", MIFe: "+DPC_MIFe
                        + ", DPC_Ca: "+DPC_Caa+ ", DPC_Ce: "+DPC_Cee+", AIS: "+DPC_AIS+", ADS: "+DPC_ADS+"\n";

            }
        }
        out.write(output);

    }

    public void calculateDistinctMIFaMIFeWhenDifferentiate(){//若算法区分不同的微服务，在统计完所有微服务间依赖矩阵及所有MIF值后计算所有MIFa和MIFe
        Iterator<Double> itMIF = distinctMIFMeasurements.iterator();
        while (itMIF.hasNext()){//初始化第一个微服务的MIFa和MIFe
            double MIF = itMIF.next();
            distinctMIFaMeasurements.add(MIF*entitiesCount);
            distinctMIFeMeasurements.add(MIF*interfacesCount);
        }

        HashSet<Double> temp2MIFa = new HashSet<Double>();
        HashSet<Double> temp2MIFe = new HashSet<Double>();

        for(int i= 1;i<microservicesCount-1;i++){// 执行剩余n-2次MIF值的累加，每次从distinctMIFMeasurements里选择一个
            Iterator<Double> itMIFa = distinctMIFaMeasurements.iterator();
            while (itMIFa.hasNext()){
                double curNext = itMIFa.next();
                Iterator<Double> it = distinctMIFMeasurements.iterator();
                while (it.hasNext()){
                    double curMIFa = curNext+it.next()*entitiesCount;
                    temp2MIFa.add(curMIFa);
                }
            }
            distinctMIFaMeasurements.clear();
            distinctMIFaMeasurements.addAll(temp2MIFa);
            temp2MIFa.clear();

            Iterator<Double> itMIFe = distinctMIFeMeasurements.iterator();
            while (itMIFe.hasNext()){
                double curNext = itMIFe.next();
                Iterator<Double> it = distinctMIFMeasurements.iterator();
                while (it.hasNext()){
                    double curMIFe = curNext+it.next()*interfacesCount;
                    temp2MIFe.add(curMIFe);
                }
            }
            distinctMIFeMeasurements.clear();
            distinctMIFeMeasurements.addAll(temp2MIFe);
            temp2MIFe.clear();

        }
        // 此处省略将MIFa/MIFe值除以AllEntities和AllInterfaces，由于该值对所有MIFa/MIFe都相等，因此并不影响distinctMIF values

    }

    public void calculateDistinctCaCeWhenDifferentiate(){
        distinctCaaMeasurements.addAll(distinctCaMeasurements);
        distinctCeeMeasurements.addAll(distinctCeMeasurements);

        HashSet<Double> temp2Ca = new HashSet<Double>();
        HashSet<Double> temp2Ce = new HashSet<Double>();
        for(int i= 1;i<microservicesCount-1;i++){// 执行剩余n-2次Ca值的累加
            Iterator<Double> itCa = distinctCaaMeasurements.iterator();
            while (itCa.hasNext()){
                double curNext = itCa.next();
                Iterator<Double> it = distinctCaMeasurements.iterator();
                while (it.hasNext()){//每次累加从distinctCaMeasurements里选择一个
                    double curCa = curNext+it.next();
                    temp2Ca.add(curCa);
                }
            }
            distinctCaaMeasurements.clear();
            distinctCaaMeasurements.addAll(temp2Ca);
            temp2Ca.clear();

            Iterator<Double> itCe = distinctCeeMeasurements.iterator();
            while (itCe.hasNext()){
                double curNext = itCe.next();
                Iterator<Double> it = distinctCeMeasurements.iterator();
                while (it.hasNext()){//每次累加从distinctCaMeasurements里选择一个
                    double curCe = curNext+it.next();
                    temp2Ce.add(curCe);
                }
            }
            distinctCeeMeasurements.clear();
            distinctCeeMeasurements.addAll(temp2Ce);
            temp2Ce.clear();
        }
    }

    public void calculateDistinctAISADSWhenDifferentiate(){
        distinctAISMeasurements.addAll(distinctADMeasurements);
        distinctADSMeasurements.addAll(distinctADMeasurements);

        HashSet<Double> temp2AIS = new HashSet<Double>();
        HashSet<Double> temp2ADS = new HashSet<Double>();
        for(int i= 1;i<microservicesCount-1;i++){// 执行剩余n-2次Ca值的累加
            Iterator<Double> itAIS = distinctAISMeasurements.iterator();
            while (itAIS.hasNext()){
                double curNext = itAIS.next();
                Iterator<Double> it = distinctADMeasurements.iterator();
                while (it.hasNext()){//每次累加从distinctADMeasurements里选择一个
                    double curAIS = curNext+it.next();
                    temp2AIS.add(curAIS);
                }
            }
            distinctAISMeasurements.clear();
            distinctAISMeasurements.addAll(temp2AIS);
            temp2AIS.clear();

            Iterator<Double> itADS = distinctADSMeasurements.iterator();
            while (itADS.hasNext()){
                double curNext = itADS.next();
                Iterator<Double> it = distinctADMeasurements.iterator();
                while (it.hasNext()){//每次累加从distinctADMeasurements里选择一个
                    double curADS = curNext+it.next();
                    temp2ADS.add(curADS);
                }
            }
            distinctADSMeasurements.clear();
            distinctADSMeasurements.addAll(temp2ADS);
            temp2ADS.clear();
        }
    }


    public void checkMatrixExistenceByRowPermutation(DependenceMatrix elementsToPermutation,
                                             int lengthOfFlexibleMatrix){
        if(lengthOfFlexibleMatrix == 1){
            checkMatrixExistenceByEntitiesColumnPermutation(elementsToPermutation,entitiesCount);//做完行变换后做列变换
        }else{
            lengthOfFlexibleMatrix -= 1;
//            System.out.println(microservicesCount+" "+ entitiesCount+" "+interfacesCount);
            checkMatrixExistenceByRowPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            for(int i = 0; i<lengthOfFlexibleMatrix;i++){
                if((lengthOfFlexibleMatrix &1) !=0)
                    elementsToPermutation.swapRowsElements(i,lengthOfFlexibleMatrix);
                else elementsToPermutation.swapRowsElements(0,lengthOfFlexibleMatrix);
                checkMatrixExistenceByRowPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            }
        }
    }

    public void checkMatrixExistenceByEntitiesColumnPermutation(DependenceMatrix elementsToPermutation,
                                                        int lengthOfFlexibleMatrix){
        if(lengthOfFlexibleMatrix == 1){
            checkMatrixExistenceByInterfacesColumnPermutation(elementsToPermutation,interfacesCount);//做完实体的列变换后做接口的列变换
        }else{
            lengthOfFlexibleMatrix -= 1;
            checkMatrixExistenceByEntitiesColumnPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            for(int i = 0; i<lengthOfFlexibleMatrix;i++){
                if((lengthOfFlexibleMatrix &1) !=0)
                    elementsToPermutation.swapColumnElements(i,lengthOfFlexibleMatrix);
                else elementsToPermutation.swapColumnElements(0,lengthOfFlexibleMatrix);
                checkMatrixExistenceByEntitiesColumnPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            }
        }
    }

    public void checkMatrixExistenceByInterfacesColumnPermutation(DependenceMatrix elementsToPermutation,
                                                          int lengthOfFlexibleMatrix){
        if(lengthOfFlexibleMatrix == 1){
            if(isMatrixInDependenceSet(elementsToPermutation)){
                matrixExistence = true;
            }
        }else{
            lengthOfFlexibleMatrix -= 1;
            checkMatrixExistenceByInterfacesColumnPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            for(int i = 0; i<lengthOfFlexibleMatrix;i++){
                if((lengthOfFlexibleMatrix &1) !=0)
                    elementsToPermutation.swapColumnElements(entitiesCount+i,entitiesCount+lengthOfFlexibleMatrix);
                else elementsToPermutation.swapColumnElements(entitiesCount+0,entitiesCount+lengthOfFlexibleMatrix);
                checkMatrixExistenceByInterfacesColumnPermutation(elementsToPermutation,lengthOfFlexibleMatrix);
            }
        }
    }

    public Boolean isMatrixInDependenceSet(DependenceMatrix elementsToPermutation){
        for (DependenceMatrix matrix: this.distinctDepMatrix){
            if(matrix.equals(elementsToPermutation))return true;
        }
        return false;
    }

    public void derivePossibleMatrix(DependenceMatrix curDepMatrix,int i,int j){
        DependenceMatrix temp = null;
        boolean diagonal = false;
        if(j<entitiesCount && i == j){//一个微服务内实体与自身的依赖关系不为1
            diagonal = true;
        }else{
            temp = new DependenceMatrix(curDepMatrix);
            temp.microservicesDepMatrix[i][j] =1;
            tryPutMatrixIntoSet(new DependenceMatrix(temp));
        }

        // 确定下次走到的矩阵位置
        if(j+1 == entitiesCount+interfacesCount){//当j走到一行的右边界时切换到下一行的左边界
            if (i+1 != entitiesCount){
                derivePossibleMatrix(new DependenceMatrix(curDepMatrix),i+1,0);
                if(!diagonal)derivePossibleMatrix(new DependenceMatrix(temp),i+1,0);
            }
        }else{
            derivePossibleMatrix(new DependenceMatrix(curDepMatrix),i,j+1);
            if(!diagonal)derivePossibleMatrix(new DependenceMatrix(temp),i,j+1);
        }

    }


    public void tryPutMatrixIntoSet(DependenceMatrix curDepMatrix){
        matrixExistence = false;
        checkMatrixExistenceByRowPermutation(new DependenceMatrix(curDepMatrix),entitiesCount);

        if (matrixExistence == false){
            distinctDepMatrix.add(curDepMatrix);
            // compute the metric values using current dependency matrix
            calculateSimulatedMIFaMIFe(curDepMatrix);
            calculateSimulatedCaCe(curDepMatrix);
            calculateSimulatedAISADS(curDepMatrix);
//            System.out.println("compute metrics");
        }
    }

    public void calculateSimulatedMIFaMIFe(DependenceMatrix depMatrix){
        double MIF = 0;
        int distanceTable[] = new int[entitiesCount];
        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<entitiesCount;i++) distanceTable[i] = -1;

        //统计被依赖微服务中被另一个微服务依赖的接口数
        int reachableInterfaces = 0;
        for(int k = 0;k<interfacesCount;k++){
            boolean reachable = false;
            for(int j=0;j<entitiesCount;j++){
                if(depMatrix.microservicesDepMatrix[j][entitiesCount+k]==1){
                    reachable=true;
                    // 计算所有微服务2中实体到当前操作的距离并更新distance表
                    updateDistanceTable(distanceTable,j,depMatrix);
                }
            }
            if(reachable)reachableInterfaces++;
        }

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        // 统计依赖微服务中依赖另一个微服务接口的实体数
        for(int i = 0;i<entitiesCount;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];
            }
        }


        if(reachableEntities!=0 && reachableInterfaces!=0) MIF = ((double)reachableInterfaces/interfacesCount)*((double)reachableEntities/entitiesCount+(double)reachableEntities/accumulatedDistance);
//        System.out.println("reachableInterfaces value: "+reachableInterfaces+" reachableEntities value: "+reachableEntities+" distance: "+accumulatedDistance);

        distinctMIFMeasurements.add(MIF);
        if(this.differentiateMicroservices) {


        }else {
//            double allEntities = (microservicesCount - 1) * entitiesCount;
//            double MIFa = (double) entitiesCount * MIF * (microservicesCount - 1) / allEntities;
            distinctMIFaMeasurements.add(MIF);

//            double allInterfaces = (microservicesCount - 1) * interfacesCount;
//            double MIFe = (double) interfacesCount * MIF * (microservicesCount - 1) / allInterfaces;
            distinctMIFeMeasurements.add(MIF);
        }
    }

    public void calculateSimulatedCaCe(DependenceMatrix depMatrix){

        // 计算microservice2中有多少个类依赖microservice1
        int dependentEntities = 0;
        for(int i=0;i<entitiesCount;i++) {
            boolean dependent = false;
            for (int k = 0; k < interfacesCount; k++) {
                if (depMatrix.microservicesDepMatrix[i][k+entitiesCount]==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentEntities++;
        }

//        double Ca = (microservicesCount-1)*dependentEntities;
//        System.out.println("distinct dependent entities: "+dependentEntities);
        distinctCaMeasurements.add((double)dependentEntities);

        int dependentInterfaces = 0;
        for(int k=0;k<interfacesCount;k++) {
            boolean dependent = false;
            for (int i = 0; i < entitiesCount; i++) {
                if (depMatrix.microservicesDepMatrix[i][k+entitiesCount]==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentInterfaces++;
        }

//        double Ce = (microservicesCount-1)*dependentInterfaces;
//        System.out.println("distinct dependent dependentInterfaces: "+dependentInterfaces);
        distinctCeMeasurements.add((double)dependentInterfaces);
    }

    public void calculateSimulatedAISADS(DependenceMatrix depMatrix){
        // 计算一个服务是否依赖另一个服务
        int dependentMicroservice = 0;
        for(int i=0;i<entitiesCount;i++) {
            for (int k = 0; k < interfacesCount; k++) {
                if (depMatrix.microservicesDepMatrix[i][k+entitiesCount]==1){
                    dependentMicroservice=1;break;
                }
            }
        }
//        double AIS = (microservicesCount-1)* dependentMicroservice;
//        System.out.println("distinct microservices: "+dependentMicroservice);
        distinctADMeasurements.add((double)dependentMicroservice);
//        double ADS = (microservicesCount-1)* dependentMicroservice;
//        distinctADSMeasurements.add(()dependentMicroservice);
    }

    public void updateDistanceTable(int distanceTable[], int entryEntityID,DependenceMatrix depMatrix){
        int distance = 1 ;
        if(distanceTable[entryEntityID]==-1|| distanceTable[entryEntityID] > distance)
            distanceTable[entryEntityID] = distance;
        putIntraInvokerEntities(distance,distanceTable,entryEntityID,depMatrix);

    }

    public void putIntraInvokerEntities(int distance,int distanceTable[], int curEntityID,DependenceMatrix depMatrix){
        if(distance == entitiesCount)return;
        distance++;
        for(int i=0;i<entitiesCount;i++){
            if(depMatrix.microservicesDepMatrix[i][curEntityID]==1){//第i个实体调用了curEntity
                if (distanceTable[i]==-1 || distanceTable[i] > distance)
                    distanceTable[i] = distance;
                putIntraInvokerEntities(distance,distanceTable,i,depMatrix);
            }

        }
    }


    class DependenceMatrix{
        int[][] microservicesDepMatrix;

        public DependenceMatrix(){
            microservicesDepMatrix = new int[entitiesCount][entitiesCount+interfacesCount];
        }

        public DependenceMatrix(DependenceMatrix template){
            microservicesDepMatrix = new int[entitiesCount][entitiesCount+interfacesCount];
            for (int i=0;i<entitiesCount;i++)
                for(int j=0;j<entitiesCount+interfacesCount;j++)
                    microservicesDepMatrix[i][j] = template.microservicesDepMatrix[i][j];
        }

        public void addRowPermutations(DependenceMatrix oneRowPermutation){
            for (int i=0;i<entitiesCount+interfacesCount;i++)
                microservicesDepMatrix[entitiesCount-1][i] = oneRowPermutation.microservicesDepMatrix[entitiesCount-1][i];
        }

        public void swapRowsElements(int i, int j){
            for (int k=0;k<entitiesCount+interfacesCount;k++){
                int temp = this.microservicesDepMatrix[i][k];
                this.microservicesDepMatrix[i][k] = this.microservicesDepMatrix[j][k];
                this.microservicesDepMatrix[j][k] = temp;
            }
        }

        public void swapColumnElements(int i, int j){
            for (int k=0;k<entitiesCount;k++){
                int temp = this.microservicesDepMatrix[k][i];
                this.microservicesDepMatrix[k][i] = this.microservicesDepMatrix[k][j];
                this.microservicesDepMatrix[k][j] = temp;
            }
        }

        public void printMatrixContent(){
            for (int i=0;i<entitiesCount;i++) {
                for (int j = 0; j < entitiesCount + interfacesCount; j++) {
                    System.out.print(this.microservicesDepMatrix[i][j] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }

        public Boolean equals(DependenceMatrix anotherMatrix){
            for (int i=0;i<entitiesCount;i++) {
                for (int j = 0; j < entitiesCount + interfacesCount; j++) {
                    if (this.microservicesDepMatrix[i][j] != anotherMatrix.microservicesDepMatrix[i][j]) return false;
                }
            }
            return true;
        }

    }

}
