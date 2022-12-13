import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

public class SimulationEfferent {
    // input
    private ArrayList<Integer> calleeInterfaceNumbers;
    private int callarEntityNumber;
    private int operationNumberLow;
    private int operationNumberUp;
    private double interDependenciesDensityUp;
    private double interDependenciesDensityLow;
    private double intraDependenciesDensityUp;
    private double intraDependenciesDensityLow;
    private double interfaceChangingProbability;
    private double operationDeletedProbability;
    private double indirectPropagationProbability;
    private int iterations;

    private IntraDepMatrix intraDepMatrix;
    private ArrayList<InterDepMatrix> interDepMatrices;
    private ArrayList<OperationsManage> operationsManages;
    private int calleeNumber;

    Random randomSeed;

    // results
    private double simulatedADS;
    private double simulatedCe;
    private double simulatedMCIee;
    private int depI;
    private int accumulatedInterfaces;
//    private int[] DepE;
    private int accumulatedDistance;
    private int accumulatedReachableEntities;
    private int distanceTable[];

    private double[] accumulatedDCFArray;
    private double[] accumulatedDCSArray;
    private double[] accumulatedICFArray;
    private double[] accumulatedICSArray;
    private double[] accumulatedOCFArray;
    private double[] accumulatedOCSArray;

    private int accumulatedDCF = 0;
    private int accumulatedDCS = 0;
    private int accumulatedICF = 0;
    private int accumulatedICS = 0;
    private int accumulatedOCF = 0;
    private int accumulatedOCS = 0;

    private int currentDCF;
    private HashSet<Integer> directlyAffectedFiles;
    private int currentICF;
    private HashSet<Integer> indirectlyAffectedFiles;

    /**
     * Step 1: get input

     */
    private SimulationEfferent(int calleeNumber, int operationNumberLow, int operationNumberUp,
                               double interDependenciesDensityLow, double interDependenciesDensityUp, double intraDependenciesDensityLow, double intraDependenciesDensityUp, int iterations,
                               double interfaceChangingProbability, double operationDeletedProbability, double indirectPropagationProbability){
        this.calleeNumber = calleeNumber;
        this.operationNumberLow = operationNumberLow;
        this.operationNumberUp = operationNumberUp;
        this.interDependenciesDensityLow = interDependenciesDensityLow;
        this.intraDependenciesDensityUp = intraDependenciesDensityUp;
        this.interDependenciesDensityUp = interDependenciesDensityUp;
        this.intraDependenciesDensityLow = intraDependenciesDensityLow;
        this.interfaceChangingProbability = interfaceChangingProbability;
        this.operationDeletedProbability = operationDeletedProbability;
        this.indirectPropagationProbability = indirectPropagationProbability;
        this.iterations = iterations;
        this.randomSeed = new Random();
    }

    public void simulatingChanges(Sheet sheet, int rowIndex){
//        System.out.println("generating dependency matrix");
        this.generateDepMatrix(); // step 2
//        System.out.println("generating operation sets");
        this.generateOperationSet();//step 3
        this.depI = 0;
//        this.DepE = new int[this.callarEntityNumber];
        this.accumulatedDistance = 0;
        this.accumulatedReachableEntities = 0;
        this.accumulatedInterfaces = 0;
        this.distanceTable = new int[this.callarEntityNumber];
        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<this.callarEntityNumber;i++) this.distanceTable[i] = -1;

//        System.out.println("calculating coupling");
        this.calculateCouplingValues(); //step 4

        this.accumulatedDCF = 0;
        this.accumulatedDCS = 0;
        this.accumulatedICF = 0;
        this.accumulatedICS = 0;
        this.accumulatedOCF = 0;
        this.accumulatedOCS = 0;

        this.accumulatedDCFArray = new double[this.calleeNumber];
        this.accumulatedDCSArray = new double[this.calleeNumber];
        this.accumulatedICFArray = new double[this.calleeNumber];
        this.accumulatedICSArray = new double[this.calleeNumber];
        this.accumulatedOCFArray = new double[this.calleeNumber];
        this.accumulatedOCSArray = new double[this.calleeNumber];

        int remainTimes = this.iterations;
        while(remainTimes-->0){ // iteration times for step 5 to step 8
//            System.out.println("changing callee");
            this.changeCalleeMicroservice();
        }

        printSimulationResult(sheet,rowIndex);
    }

    private void printSimulationResult(Sheet sheet, int rowIndex){
        int overallInterfaces = 0;
        for(int ith=0;ith<this.calleeNumber;ith++){
            overallInterfaces += this.calleeInterfaceNumbers.get(ith);
        }
//        double base = this.iterations*this.callarEntityNumber*overallInterfaces;
        double base = this.iterations*overallInterfaces;
        Util.writeToSimulationEfferentResult(sheet,rowIndex,this.callarEntityNumber,
                this.simulatedADS,this.simulatedCe,this.simulatedMCIee,
                (double)this.accumulatedDCF/base,(double)this.accumulatedDCS/base,
                (double)this.accumulatedICF/base, (double)this.accumulatedICS/base,
                (double)this.accumulatedOCF/base,(double)this.accumulatedOCS/base);

//        System.out.println(this.simulatedCe+"\t"+this.simulatedMCIee+"\t"+this.accumulatedDCFArray[0]/base+"\t"+this.accumulatedDCFArray[1]/base);
    }

    public static void main(String[] args) throws IOException {
        String resultPrefix = "data/simulation_RQ301/";

        Workbook workbook  = new XSSFWorkbook();
        Sheet sheet = Util.createSimulationEfferentResult(workbook,"round1");
        int rowIndex = 0;
        int resultNumber = 100;

        while(resultNumber-->0){
            SimulationEfferent simulation = new SimulationEfferent(10,
                    3,7,0,1,
                    0,1,1000,
                    0.02,0.33,0.02);
            simulation.simulatingChanges(sheet,rowIndex++);
        }

        Util.writeExcel(workbook,resultPrefix+"simulationEfferentResult"+"RandomSize.xlsx");


    }



    /**
     * Step 2: generate a random matrix
     */
    private void generateDepMatrix(){
        this.interDepMatrices = new ArrayList<>();
        // initialize the size of callar
//        this.callarEntityNumber = randomSeed.nextInt(25)+4; // the entity range of callar is 4-28
        this.callarEntityNumber = 20;
        this.intraDepMatrix = new IntraDepMatrix(this.callarEntityNumber);

        // initialize the intra-dependency matrix
        int intraDependenciesNumberUp = (int) Math.ceil(this.intraDependenciesDensityUp*this.callarEntityNumber*(this.callarEntityNumber-1));
        int intraDependenciesNumberLow = (int) Math.floor(this.intraDependenciesDensityLow*this.callarEntityNumber*(this.callarEntityNumber-1));
        int intraDependenciesNumber = intraDependenciesNumberLow;
        if(intraDependenciesNumberUp>intraDependenciesNumberLow)intraDependenciesNumber+=randomSeed.nextInt(intraDependenciesNumberUp-intraDependenciesNumberLow+1);
        ArrayList<Integer> intraDependenciesList = new ArrayList<>();
        for(int k=0;k<intraDependenciesNumber;k++){
            int dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
            int dependedEntity = randomSeed.nextInt(this.callarEntityNumber);
            while(intraDependenciesList.contains(dependingEntity*this.callarEntityNumber+dependedEntity)
                    ||dependingEntity==dependedEntity){
                dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
                dependedEntity = randomSeed.nextInt(this.callarEntityNumber);
            }
            intraDependenciesList.add(dependingEntity*callarEntityNumber+dependedEntity);
            this.intraDepMatrix.set(dependingEntity,dependedEntity,1);
        }

        // initialize the sizes of callee and the dependency matrices
        this.calleeInterfaceNumbers = new ArrayList<>();
        for (int i=0;i<this.calleeNumber;i++){
            int calleeInterfaceNumber = randomSeed.nextInt(4)+1; // the interface range of callee is 1-4
            this.calleeInterfaceNumbers.add(calleeInterfaceNumber);

            this.interDepMatrices.add(new InterDepMatrix(this.callarEntityNumber,calleeInterfaceNumber));

            // initialize the inter-dependency matrix
            int interDependenciesNumberUp = (int) Math.ceil(this.interDependenciesDensityUp*this.callarEntityNumber*calleeInterfaceNumber);
            int interDependenciesNumberLow = (int) Math.ceil(this.interDependenciesDensityLow*this.callarEntityNumber*calleeInterfaceNumber);
            int interDependenciesNumber = interDependenciesNumberLow;
            if(interDependenciesNumberUp>interDependenciesNumberLow)interDependenciesNumber+=randomSeed.nextInt(interDependenciesNumberUp-interDependenciesNumberLow+1);
            ArrayList<Integer> interDependenciesList = new ArrayList<>();
            for(int k=0;k<interDependenciesNumber;k++){
                int dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
                int dependedInterface = randomSeed.nextInt(calleeInterfaceNumber);
                while(interDependenciesList.contains(dependingEntity*calleeInterfaceNumber+dependedInterface)){
                    dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
                    dependedInterface = randomSeed.nextInt(calleeInterfaceNumber);
                }
                interDependenciesList.add(dependingEntity*calleeInterfaceNumber+dependedInterface);
                this.interDepMatrices.get(i).set(dependingEntity,dependedInterface,1);
            }
        }
    }


    /**
     * Step 3: generate operations of interfaces and
     * the directly dependent operations of entities
     */
    private void generateOperationSet(){
        this.operationsManages = new ArrayList<>();
        for (int ith=0;ith<this.calleeNumber;ith++){
            int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);

            // initialize the operationManage
            this.operationsManages.add(new OperationsManage(this.callarEntityNumber,calleeInterfaceNumber));
            // generate random operations of all interfaces
            for(int i=0;i<calleeInterfaceNumber;i++){
                this.operationsManages.get(ith).interfaceOperationSet[i] = this.operationNumberLow+this.randomSeed.nextInt(this.operationNumberUp-this.operationNumberLow+1);
            }
            // generate randomly the dependent operations of each entity
            for(int j=0;j<calleeInterfaceNumber;j++){
                // derive the operation number of the current interface
                int operationNumber = this.operationsManages.get(ith).interfaceOperationSet[j];
                for(int i=0;i<this.callarEntityNumber;i++){
                    if(this.interDepMatrices.get(ith).get(i,j)==1){//the current entity depends on the current interface
                        this.operationsManages.get(ith).dependentOperationSet[i][j] = new OperationSet(operationNumber);
                        int dependentOperationNumber = randomSeed.nextInt(operationNumber)+1; // at least one operation is depended upon
                        for(int k=0;k<dependentOperationNumber;k++){
                            int currentDependentOperation = randomSeed.nextInt(operationNumber);
                            if(this.operationsManages.get(ith).dependentOperationSet[i][j].operations[currentDependentOperation]==1){
                                currentDependentOperation = randomSeed.nextInt(operationNumber);
                            }
                            this.operationsManages.get(ith).dependentOperationSet[i][j].operations[currentDependentOperation] = 1;
                        }

                    }
                }
            }

        }
    }

    /**
     * Step 4: calculate the coupling values from callar to callee
     */
    private void calculateCouplingValues(){
        double ACT2MicroservicesMatrix = 0;
        double Ce2MicroservicesMatrix = 0;

        for(int ith=0;ith<this.calleeNumber;ith++){
            ACT2MicroservicesMatrix += this.calculateSimulatedACT(ith);
            Ce2MicroservicesMatrix += this.calculateSimulatedCeT(ith);
            this.calculateSimulatedMCI(ith);
        }

        this.simulatedADS = ACT2MicroservicesMatrix;
        this.simulatedCe = Ce2MicroservicesMatrix;
        int reachableEntities = 0;
        int accumulatedDistance = 0;
        for(int i=0;i<this.callarEntityNumber;i++){
            if(this.distanceTable[i]!=-1){
                reachableEntities ++;
                accumulatedDistance += this.distanceTable[i];
            }
        }

        if(reachableEntities!=0){
            this.simulatedMCIee = ((double) this.depI / (this.accumulatedInterfaces)) *
                    ((double) reachableEntities / this.callarEntityNumber
                            + (double) reachableEntities / accumulatedDistance);
        }

    }

    /**
     * Step 5: determine whether to change interfaces of callee
     */
    private void changeCalleeMicroservice(){
        this.directlyAffectedFiles = new HashSet<Integer>();
        this.indirectlyAffectedFiles = new HashSet<Integer>(); // for recording the indirectly affected files

        for(int ith=0;ith<this.calleeNumber;ith++){
            int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);

            boolean [] isChangingInterface = new boolean[calleeInterfaceNumber];
            boolean isChangingInterfaceInCallee = false;
            for(int i=0;i<calleeInterfaceNumber;i++){
                if(randomSeed.nextInt(1000)<this.interfaceChangingProbability*1000){
                    isChangingInterface[i] = true;
                    isChangingInterfaceInCallee = true;
                }
            }
            if(isChangingInterfaceInCallee) // go to step 6
                checkRippleEffect(ith,isChangingInterface);
            // else: the current ripple effects are zeros
        }

        this.accumulatedDCS += this.directlyAffectedFiles.size();
        this.accumulatedICS += this.indirectlyAffectedFiles.size();
        this.indirectlyAffectedFiles.addAll(this.directlyAffectedFiles);
        this.accumulatedOCS += this.indirectlyAffectedFiles.size();
    }

    /**
     * Step 6: determine whether there is entity in callar invoking the interface deleting operations
     * @param isChangingInterface
     */
    private void checkRippleEffect(int ith, boolean [] isChangingInterface){
        int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);

        boolean [] isDependedChangingInterface = new boolean[calleeInterfaceNumber];
        boolean isChangingInterfaceDepended = false;

        for(int i=0;i<calleeInterfaceNumber;i++){
            if(isChangingInterface[i]){ // an operation deletion event is to be happened
                for(int j=0;j<this.callarEntityNumber;j++){
                    if(this.interDepMatrices.get(ith).get(j,i)==1){
                        isChangingInterfaceDepended = true;
                        isDependedChangingInterface[i] = true; // the changing interface is depended on
                    }
                }
            }
        }
        if(isChangingInterfaceDepended) // go to step 7
            quantifyDirectRippleEffect(ith, isDependedChangingInterface);
        // else: the current ripple effects are zeros

    }

    /**
     * Step 7: calculate the impacts of deleted operations in callee
     * @param isDependedChangingInterface
     */
    private void quantifyDirectRippleEffect(int ith, boolean [] isDependedChangingInterface) {
        int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);
        this.currentDCF = 0;
        System.out.println("initialize DCF");
        HashSet<Integer> currentCalleeDirectlyAffectedFiles = new HashSet<Integer>();

        for (int i = 0; i < calleeInterfaceNumber; i++) {
            if (isDependedChangingInterface[i]) { // determine the ripple effects of the current changing interface (that is depended on by callar)
                // derive the entities that depend on this interface
                ArrayList<Integer> dependingEntity = new ArrayList<Integer>();
                for (int j = 0; j < callarEntityNumber; j++) {
                    if (this.interDepMatrices.get(ith).get(j,i) == 1) {
                        dependingEntity.add(j);
                    }
                }

                // generate the deleted operations
                int operationNumber = this.operationsManages.get(ith).interfaceOperationSet[i];
                for (int k = 0; k < operationNumber; k++) {
                    if (randomSeed.nextInt(1000) < this.operationDeletedProbability * 1000) {

                        for (int t = 0; t < dependingEntity.size(); t++) { // for each entity that depends on the changing interface
                            if (this.operationsManages.get(ith).dependentOperationSet[dependingEntity.get(t)][i].operations[k] == 1) {
                                // the current entity depends on the deleted operation
                                this.currentDCF++;
                                this.directlyAffectedFiles.add(dependingEntity.get(t));
                                currentCalleeDirectlyAffectedFiles.add(dependingEntity.get(t));

                            }
                        }
                    }
                }
            }

        }

        System.out.println("trying to accumulate DCF ...");
        if (this.currentDCF != 0) {
            this.accumulatedDCF += this.currentDCF;
            this.accumulatedDCFArray[ith] += this.currentDCF;
//            this.accumulatedDCS += this.directlyAffectedFiles.size()/callarEntityNumber;
            this.quantifyIndirectRippleEffectDFS(ith, currentCalleeDirectlyAffectedFiles);// go to step 8
        }// else: the current direct ripple effects are zeros

    }


    /**
     * Step 8: calculate the indirect impacts of deleted operations in callee
     * @param directlyAffectedFiles
     */
    private void quantifyIndirectRippleEffectDFS(int ith, HashSet<Integer> directlyAffectedFiles ) {
        this.currentICF = 0;

        for (Integer directlyAffectedFile : directlyAffectedFiles) {
            Stack<Integer> rippleStack = new Stack<>();
            boolean[] fileAffected = new boolean[this.callarEntityNumber];
            fileAffected[directlyAffectedFile] = true;

            for (int i = 0; i < this.callarEntityNumber; i++)
                if (this.intraDepMatrix.get(i,directlyAffectedFile) == 1) rippleStack.push(i);

            while (!rippleStack.isEmpty()) {
                Integer file = rippleStack.pop();

                int changeRandom = randomSeed.nextInt(1000);
                if (changeRandom< this.indirectPropagationProbability * 1000
                        && fileAffected[file] == false) {
                    this.indirectlyAffectedFiles.add(file);
                    fileAffected[file] = true;
                    this.currentICF++;

                    for (int j = 0; j < callarEntityNumber; j++) {
                        if (this.intraDepMatrix.get(j,file) == 1) rippleStack.push(j);
                    }
                }
            }
        }

        this.accumulatedICF += this.currentICF;
        this.accumulatedICFArray[ith] += this.currentICF;

        this.accumulatedOCF += (this.currentICF+this.currentDCF);
        this.accumulatedOCFArray[ith] += (this.currentICF+this.currentDCF);
    }


    private void updateDistanceTable(int ith, int entryEntityID){
        int distance = 1 ;
        if(this.distanceTable[entryEntityID]==-1|| this.distanceTable[entryEntityID] > distance)
            this.distanceTable[entryEntityID] = distance;
        putIntraInvokerEntities(ith,distance,entryEntityID);

    }

    private void putIntraInvokerEntities(int ith, int distance, int curEntityID){
        if(distance == this.callarEntityNumber)return;
        distance++;
        for(int i=0;i<this.callarEntityNumber;i++){
            if(this.intraDepMatrix.get(i,curEntityID)==1){//向上回溯调用了curEntity的callar内实体，并更新其距离表
                if (this.distanceTable[i]==-1 || this.distanceTable[i] > distance){
                    this.distanceTable[i] = distance;
                    putIntraInvokerEntities(ith,distance,i);
                }
            }
        }
    }

    private void calculateSimulatedMCI(int ith){
        int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);

        //统计callee微服务中被callar微服务依赖的接口数
        int reachableInterfaces = 0;
        for(int k = 0;k<calleeInterfaceNumber;k++){
            boolean reachable = false;
            for(int j=0;j<this.callarEntityNumber;j++){
                if(this.interDepMatrices.get(ith).get(j,k)==1){
                    reachable=true;
                    // 计算所有微服务2中实体到当前操作的距离并更新distance表
                    updateDistanceTable(ith,j);
                }
            }
            if(reachable)reachableInterfaces++;
        }

        this.depI += reachableInterfaces;
        this.accumulatedInterfaces += calleeInterfaceNumber;

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        // 统计依赖微服务中依赖另一个微服务接口的实体数
        for(int i = 0;i<this.callarEntityNumber;i++){
            if (this.distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += this.distanceTable[i];
            }
        }

        this.accumulatedDistance += accumulatedDistance;
        this.accumulatedReachableEntities += reachableEntities;

    }

    private double calculateSimulatedCeT(int ith){
        // calculating the number of interfaces in the callee microservice that is depended on by the callar
        int dependentInterfaces = 0;

        int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);
        for(int k=0;k<calleeInterfaceNumber;k++) {
            boolean dependent = false;
            for (int i = 0; i < this.callarEntityNumber; i++) {
                if (this.interDepMatrices.get(ith).get(i,k)==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentInterfaces++;
        }
        return dependentInterfaces;
    }

    private double calculateSimulatedACT(int ith){
        // calculating whether the callar microservice depends on the callee
        int dependentMicroservice = 0;
        for(int i=0;i<this.callarEntityNumber;i++) {
            int calleeInterfaceNumber = this.calleeInterfaceNumbers.get(ith);
            for (int k = 0; k < calleeInterfaceNumber; k++) {
                if (this.interDepMatrices.get(ith).get(i,k)==1){
                    dependentMicroservice=1;break;
                }
            }
        }
        return dependentMicroservice;
    }

    class OperationSet{
        int[] operations;
        private OperationSet(int operationNumber){
            operations = new int[operationNumber];
        }
    }

    class IntraDepMatrix{
        int[][] intraDepMatrix;
        private IntraDepMatrix(int entityNumber){
            intraDepMatrix = new int[entityNumber][entityNumber];
        }

        public void set(int row, int column, int value){
            this.intraDepMatrix[row][column] = value;
        }
        public int get(int row, int column){
            return this.intraDepMatrix[row][column];
        }
    }

    class InterDepMatrix{
        int[][] interDepMatrix;
        private InterDepMatrix(int entityNumber, int interfaceNumber){
            interDepMatrix = new int[entityNumber][interfaceNumber];
        }

        public void set(int row, int column, int value){
            this.interDepMatrix[row][column] = value;
        }
        public int get(int row, int column){
            return this.interDepMatrix[row][column];
        }

    }

    class OperationsManage{
        int[] interfaceOperationSet;
        OperationSet[][] dependentOperationSet; //of per entity and per interface

        private OperationsManage(int entityNumber, int interfaceNumber){
            this.interfaceOperationSet = new int[interfaceNumber];
            this.dependentOperationSet = new OperationSet[entityNumber][interfaceNumber];
        }

    }

}
