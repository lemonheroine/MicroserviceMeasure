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
    private ArrayList<Integer> callarEntityNumbers;
    private int calleeInterfaceNumber;
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

    private ArrayList<IntraDepMatrix> intraDepMatrices;
    private ArrayList<InterDepMatrix> interDepMatrices;
    private ArrayList<OperationsManage> operationsManages;
    private int callarNumber;

    Random randomSeed;

    // results
    private double simulatedAIS;
    private double simulatedCa;
    private double simulatedMCIor;


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
    private SimulationEfferent(int callarNumber, int operationNumberLow, int operationNumberUp,
                               double interDependenciesDensityLow, double interDependenciesDensityUp, double intraDependenciesDensityLow, double intraDependenciesDensityUp, int iterations,
                               double interfaceChangingProbability, double operationDeletedProbability, double indirectPropagationProbability){
        this.callarNumber = callarNumber;
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
        System.out.println("generating dependency matrix");
        this.generateDepMatrix(); // step 2
        System.out.println("generating operation sets");
        this.generateOperationSet();//step 3
        System.out.println("calculating coupling");
        this.calculateCouplingValues(); //step 4

        this.accumulatedDCF = 0;
        this.accumulatedDCS = 0;
        this.accumulatedICF = 0;
        this.accumulatedICS = 0;
        this.accumulatedOCF = 0;
        this.accumulatedOCS = 0;
        int remainTimes = this.iterations;
        while(remainTimes-->0){ // iteration times for step 5 to step 8
            System.out.println("changing callee");
            this.changeCalleeMicroservice();
        }

        printSimulationResult(sheet,rowIndex);
    }

    private void printSimulationResult(Sheet sheet, int rowIndex){

        Util.writeToSimulationAfferentResult(sheet,rowIndex,this.calleeInterfaceNumber,
                this.simulatedAIS,this.simulatedCa,this.simulatedMCIor,
                (double)this.accumulatedDCF,(double)this.accumulatedDCS,
                (double)this.accumulatedICF, (double)this.accumulatedICS,
                (double)this.accumulatedOCF,(double)this.accumulatedOCS);

    }

    public static void main(String[] args) throws IOException {
        String resultPrefix = "simulation_RQ301/";

        Workbook workbook  = new XSSFWorkbook();
        Sheet sheet = Util.createSimulationAfferentResult(workbook,"round1");
        int rowIndex = 0;
        int resultNumber = 100;
        Random randomSeed = new Random();

        while(resultNumber-->0){
//            int callarSize = randomSeed.nextInt(25)+4; // the entity range of callar is 4-28
//            int calleeSize = randomSeed.nextInt(4)+1; // the interface range of callee is 1-4

            SimulationEfferent simulation = new SimulationEfferent(10,
                    3,7,0,1,
                    0,1,1000,
                    0.1,0.56,0.02);
            simulation.simulatingChanges(sheet,rowIndex++);
        }

        Util.writeExcel(workbook,resultPrefix+"simulationResult"+"RandomSize.xlsx");


    }



    /**
     * Step 2: generate a random matrix
     */
    private void generateDepMatrix(){
        this.interDepMatrices = new ArrayList<>();
        this.intraDepMatrices = new ArrayList<>();
        // initialize the size of callee
        this.calleeInterfaceNumber = randomSeed.nextInt(4)+1; // the interface range of callee is 1-4
        // initialize the sizes of callar and the dependency matrices
        this.callarEntityNumbers = new ArrayList<>();
        for (int i=0;i<this.callarNumber;i++){
            int callarEntityNumber = randomSeed.nextInt(25)+4; // the entity range of callar is 4-28
            this.callarEntityNumbers.add(callarEntityNumber);

            this.interDepMatrices.add(new InterDepMatrix(callarEntityNumber,this.calleeInterfaceNumber));
            this.intraDepMatrices.add(new IntraDepMatrix(callarEntityNumber));

            // initialize the intra-dependency matrix
            int intraDependenciesNumberUp = (int) Math.ceil(this.intraDependenciesDensityUp*callarEntityNumber*(callarEntityNumber-1));
            int intraDependenciesNumberLow = (int) Math.floor(this.intraDependenciesDensityLow*callarEntityNumber*(callarEntityNumber-1));
            int intraDependenciesNumber = intraDependenciesNumberLow;
            if(intraDependenciesNumberUp>intraDependenciesNumberLow)intraDependenciesNumber+=randomSeed.nextInt(intraDependenciesNumberUp-intraDependenciesNumberLow+1);
            ArrayList<Integer> intraDependenciesList = new ArrayList<>();
            for(int k=0;k<intraDependenciesNumber;k++){
                int dependingEntity = randomSeed.nextInt(callarEntityNumber);
                int dependedEntity = randomSeed.nextInt(callarEntityNumber);
                while(intraDependenciesList.contains(dependingEntity*callarEntityNumber+dependedEntity)
                        ||dependingEntity==dependedEntity){
                    dependingEntity = randomSeed.nextInt(callarEntityNumber);
                    dependedEntity = randomSeed.nextInt(callarEntityNumber);
                }
                intraDependenciesList.add(dependingEntity*callarEntityNumber+dependedEntity);
                this.intraDepMatrices.get(i).set(dependingEntity,dependedEntity,1);
            }

            // initialize the inter-dependency matrix
            int interDependenciesNumberUp = (int) Math.ceil(this.interDependenciesDensityUp*callarEntityNumber*this.calleeInterfaceNumber);
            int interDependenciesNumberLow = (int) Math.ceil(this.interDependenciesDensityLow*callarEntityNumber*this.calleeInterfaceNumber);
            int interDependenciesNumber = interDependenciesNumberLow;
            if(interDependenciesNumberUp>interDependenciesNumberLow)interDependenciesNumber+=randomSeed.nextInt(interDependenciesNumberUp-interDependenciesNumberLow+1);
            ArrayList<Integer> interDependenciesList = new ArrayList<>();
            for(int k=0;k<interDependenciesNumber;k++){
                int dependingEntity = randomSeed.nextInt(callarEntityNumber);
                int dependedInterface = randomSeed.nextInt(this.calleeInterfaceNumber);
                while(interDependenciesList.contains(dependingEntity*this.calleeInterfaceNumber+dependedInterface)){
                    dependingEntity = randomSeed.nextInt(callarEntityNumber);
                    dependedInterface = randomSeed.nextInt(this.calleeInterfaceNumber);
                }
                interDependenciesList.add(dependingEntity*this.calleeInterfaceNumber+dependedInterface);
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
        for (int ith=0;ith<this.callarNumber;ith++){
            int callarEntityNumber = this.callarEntityNumbers.get(ith);

            // initialize the operationManage
            this.operationsManages.add(new OperationsManage(callarEntityNumber,calleeInterfaceNumber));
            // generate random operations of all interfaces
            for(int i=0;i<this.calleeInterfaceNumber;i++){
                this.operationsManages.get(ith).interfaceOperationSet[i] = this.operationNumberLow+this.randomSeed.nextInt(this.operationNumberUp-this.operationNumberLow+1);
            }
            // generate randomly the dependent operations of each entity
            for(int j=0;j<this.calleeInterfaceNumber;j++){
                // derive the operation number of the current interface
                int operationNumber = this.operationsManages.get(ith).interfaceOperationSet[j];
                for(int i=0;i<callarEntityNumber;i++){
                    if(this.intraDepMatrices.get(ith).get(i,j)==1){//the current entity depends on the current interface
                        this.operationsManages.get(ith).dependentOperationSet[i][j] = new OperationSet(operationNumber);
                        for(int k=0;k<operationNumber;k++){
                            if(randomSeed.nextBoolean()){// the current operation in the interface is depended by the current entity
                                this.operationsManages.get(ith).dependentOperationSet[i][j].dependentOperations[k] = 1;
                            }
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
        double Ca2MicroservicesMatrix = 0;
        double MCI2MicroservicesMatrix = 0;

        for(int ith=0;ith<this.callarNumber;ith++){
            System.out.println("calculating ACT");
            ACT2MicroservicesMatrix += this.calculateSimulatedACT(ith);
            System.out.println("calculating CaT");
            Ca2MicroservicesMatrix += this.calculateSimulatedCaT(ith);
            System.out.println("calculating MCI");
            MCI2MicroservicesMatrix += this.calculateSimulatedMCI(ith);
        }

        this.simulatedAIS = ACT2MicroservicesMatrix;
        this.simulatedCa = Ca2MicroservicesMatrix;
        this.simulatedMCIor = MCI2MicroservicesMatrix/this.callarNumber;

    }

    /**
     * Step 5: determine whether to change interfaces of callee
     */
    private void changeCalleeMicroservice(){
        boolean [] isChangingInterface = new boolean[this.calleeInterfaceNumber];
        boolean isChangingInterfaceInCallee = false;
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            if(randomSeed.nextInt(1000)<this.interfaceChangingProbability*1000){
                isChangingInterface[i] = true;
                isChangingInterfaceInCallee = true;
            }
        }
        if(isChangingInterfaceInCallee) // go to step 6
            checkRippleEffect(isChangingInterface);
        // else: the current ripple effects are zeros
    }

    /**
     * Step 6: determine whether there is entity in callar invoking the interface deleting operations
     * @param isChangingInterface
     */
    private void checkRippleEffect(boolean [] isChangingInterface){
        for(int ith=0;ith<this.callarNumber;ith++){
            int callarEntityNumber = this.callarEntityNumbers.get(ith);

            boolean [] isDependedChangingInterface = new boolean[this.calleeInterfaceNumber];
            boolean isChangingInterfaceDepended = false;

            for(int i=0;i<this.calleeInterfaceNumber;i++){
                if(isChangingInterface[i]){ // an operation deletion event is to be happened
                    for(int j=0;j<callarEntityNumber;j++){
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
    }

    /**
     * Step 7: calculate the impacts of deleted operations in callee
     * @param isDependedChangingInterface
     */
    private void quantifyDirectRippleEffect(int ith, boolean [] isDependedChangingInterface) {
        this.currentDCF = 0;
        this.directlyAffectedFiles = new HashSet<Integer>();
        int callarEntityNumber = this.callarEntityNumbers.get(ith);

        for (int i = 0; i < this.calleeInterfaceNumber; i++) {
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

                int deletedNumber = 0;
                for (int k = 0; k < operationNumber; k++) {
                    if (randomSeed.nextInt(1000) < this.operationDeletedProbability * 1000) {
                        deletedNumber++;
                        for (int t = 0; t < dependingEntity.size(); t++) { // for each entity that depends on the changing interface
                            System.out.println(this.operationsManages.get(ith).dependentOperationSet[dependingEntity.get(t)][i]);
                            if (this.operationsManages.get(ith).dependentOperationSet[dependingEntity.get(t)][i].dependentOperations[k] == 1) {
                                // the current entity depends on the deleted operation
                                currentDCF++;
                                this.directlyAffectedFiles.add(dependingEntity.get(t));

                            }
                        }
                    }
                }
                System.out.println("deleted operation number: " + deletedNumber + " in " + operationNumber);
            }

        }

        if (this.currentDCF != 0) {
            this.accumulatedDCF += this.currentDCF/callarEntityNumber;
            this.accumulatedDCS += this.directlyAffectedFiles.size()/callarEntityNumber;
            this.quantifyIndirectRippleEffectDFS(ith, this.directlyAffectedFiles);// go to step 8
        }// else: the current direct ripple effects are zeros

    }


    /**
     * Step 8: calculate the indirect impacts of deleted operations in callee
     * @param directlyAffectedFiles
     */
    private void quantifyIndirectRippleEffectDFS(int ith, HashSet<Integer> directlyAffectedFiles ) {
        this.indirectlyAffectedFiles = new HashSet<Integer>(); // for recording the indirectly affected files
        this.currentICF = 0;
        int callarEntityNumber = this.callarEntityNumbers.get(ith);

        for (Integer directlyAffectedFile : directlyAffectedFiles) {
            Stack<Integer> rippleStack = new Stack<>();
            boolean[] fileAffected = new boolean[callarEntityNumber];
            fileAffected[directlyAffectedFile] = true;

            for (int i = 0; i < callarEntityNumber; i++)
                if (this.intraDepMatrices.get(ith).get(i,directlyAffectedFile) == 1) rippleStack.push(i);

            while (!rippleStack.isEmpty()) {
                Integer file = rippleStack.pop();

                int changeRandom = randomSeed.nextInt(1000);
                if (changeRandom< this.indirectPropagationProbability * 1000
                        && fileAffected[file] == false) {
                    this.indirectlyAffectedFiles.add(file);
                    fileAffected[file] = true;
                    this.currentICF++;

                    for (int j = 0; j < callarEntityNumber; j++) {
                        if (this.intraDepMatrices.get(ith).get(j,file) == 1) rippleStack.push(j);
                    }
                }
            }
        }
        if(this.indirectlyAffectedFiles.size()>this.directlyAffectedFiles.size()){
            System.out.println("something wrong");
        }
        this.accumulatedICF += this.currentICF/callarEntityNumber;
        this.accumulatedICS += this.indirectlyAffectedFiles.size()/callarEntityNumber;

        this.accumulatedOCF += (this.currentICF+this.currentDCF)/callarEntityNumber;
        this.indirectlyAffectedFiles.addAll(this.directlyAffectedFiles);
        this.accumulatedOCS += this.indirectlyAffectedFiles.size()/callarEntityNumber;

    }


    private void updateDistanceTable(int ith, int distanceTable[], int entryEntityID){
        int distance = 1 ;
        if(distanceTable[entryEntityID]==-1|| distanceTable[entryEntityID] > distance)
            distanceTable[entryEntityID] = distance;
        putIntraInvokerEntities(ith,distance,distanceTable,entryEntityID);

    }

    private void putIntraInvokerEntities(int ith, int distance, int distanceTable[], int curEntityID){
        int callarEntityNumber = this.callarEntityNumbers.get(ith);
        if(distance == callarEntityNumber)return;
        distance++;
        for(int i=0;i<callarEntityNumber;i++){
            if(this.intraDepMatrices.get(ith).get(i,curEntityID)==1){//向上回溯调用了curEntity的callar内实体，并更新其距离表
                if (distanceTable[i]==-1 || distanceTable[i] > distance){
                    distanceTable[i] = distance;
                    putIntraInvokerEntities(ith,distance,distanceTable,i);
                }
            }
        }
    }

    private double calculateSimulatedMCI(int ith){
        int callarEntityNumber = this.callarEntityNumbers.get(ith);
        int distanceTable[] = new int[callarEntityNumber];
        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<callarEntityNumber;i++) distanceTable[i] = -1;

        //统计callee微服务中被callar微服务依赖的接口数
        int reachableInterfaces = 0;
        for(int k = 0;k<this.calleeInterfaceNumber;k++){
            boolean reachable = false;
            for(int j=0;j<callarEntityNumber;j++){
                if(this.interDepMatrices.get(ith).get(j,k)==1){
                    reachable=true;
                    // 计算所有微服务2中实体到当前操作的距离并更新distance表
                    updateDistanceTable(ith,distanceTable,j);
                }
            }
            if(reachable)reachableInterfaces++;
        }

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        // 统计依赖微服务中依赖另一个微服务接口的实体数
        for(int i = 0;i<callarEntityNumber;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];
            }
        }

        return ((double)reachableInterfaces/this.calleeInterfaceNumber)*((double)reachableEntities/callarEntityNumber+(double)reachableEntities/accumulatedDistance);

    }

    private double calculateSimulatedCaT(int ith){
        // calculating the number of entities in the callar microservice depends on the callee
        int dependentEntities = 0;
        int callarEntityNumber = this.callarEntityNumbers.get(ith);
        for(int i=0;i<callarEntityNumber;i++) {
            boolean dependent = false;
            for (int k = 0; k < this.calleeInterfaceNumber; k++) {
                if (this.interDepMatrices.get(ith).get(i,k)==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentEntities++;
        }
        return dependentEntities;
    }

    private double calculateSimulatedCeT(int ith){
        // calculating the number of interfaces in the callee microservice that is depended on by the callar
        int dependentInterfaces = 0;
        int callarEntityNumber = this.callarEntityNumbers.get(ith);
        for(int k=0;k<this.calleeInterfaceNumber;k++) {
            boolean dependent = false;
            for (int i = 0; i < callarEntityNumber; i++) {
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
        int callarEntityNumber = this.callarEntityNumbers.get(ith);
        for(int i=0;i<callarEntityNumber;i++) {
            for (int k = 0; k < this.calleeInterfaceNumber; k++) {
                if (this.interDepMatrices.get(ith).get(i,k)==1){
                    dependentMicroservice=1;break;
                }
            }
        }
        return dependentMicroservice;
    }

    class OperationSet{
        int[] dependentOperations;
        private OperationSet(int operationNumber){
            dependentOperations = new int[operationNumber];
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
