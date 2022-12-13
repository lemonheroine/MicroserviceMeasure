import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.*;

public class SimulationBasic {
    // input
    private int callarEntityNumber;
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

    private IntraDepMatrix intraDepMatrix;
    private InterDepMatrix interDepMatrix;
    private OperationsManage operationsManage;

    Random randomSeed;

    // results
    private double simulatedACT;
    private double simulatedCaT;
    private double simulatedCeT;
    private double simulatedMCI;


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
     * @param callarEntityNumber
     * @param calleeInterfaceNumber
     */
    private SimulationBasic(int callarEntityNumber, int calleeInterfaceNumber, int operationNumberLow, int operationNumberUp,
                            double interDependenciesDensityLow, double interDependenciesDensityUp, double intraDependenciesDensityLow, double intraDependenciesDensityUp, int iterations,
                            double interfaceChangingProbability, double operationDeletedProbability, double indirectPropagationProbability){
        this.callarEntityNumber = callarEntityNumber;
        this.calleeInterfaceNumber = calleeInterfaceNumber;
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
        double base = this.iterations*this.callarEntityNumber*this.calleeInterfaceNumber;
        Util.writeToSimulationResult(sheet,rowIndex,this.callarEntityNumber,this.calleeInterfaceNumber,
                this.simulatedACT,this.simulatedCaT,this.simulatedCeT,this.simulatedMCI,
                (double)this.accumulatedDCF/base,(double)this.accumulatedDCS/base,
                (double)this.accumulatedICF/base, (double)this.accumulatedICS/base,
                (double)this.accumulatedOCF/base,(double)this.accumulatedOCS/base);

    }

    public static void main(String[] args) throws IOException {
        String resultPrefix = "data/simulation_RQ301/";

        Workbook workbook  = new XSSFWorkbook();
        Sheet sheet = Util.createSimulationResult(workbook,"round1");
        int rowIndex = 0;
        int resultNumber = 100;
        Random randomSeed = new Random();

        while(resultNumber-->0){
            int callarSize = randomSeed.nextInt(25)+4; // the entity range of callar is 4-28
           int calleeSize = randomSeed.nextInt(4)+1; // the interface range of callee is 1-4

            System.out.println("callar size: "+ callarSize+", callee size: "+ calleeSize);
            SimulationBasic simulation = new SimulationBasic(callarSize,calleeSize,
                    3,7,0,1,
                    0,1,1000,
                    0.02,0.33,0.02);
            simulation.simulatingChanges(sheet,rowIndex++);
        }

        Util.writeExcel(workbook,resultPrefix+"simulationResult"+"RandomSize.xlsx");


    }



    /**
     * Step 2: generate a random matrix
     */
    private void generateDepMatrix(){
        this.interDepMatrix = new InterDepMatrix(callarEntityNumber,calleeInterfaceNumber);
        this.intraDepMatrix = new IntraDepMatrix(callarEntityNumber);
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
            intraDependenciesList.add(dependingEntity*this.callarEntityNumber+dependedEntity);
            this.intraDepMatrix.set(dependingEntity,dependedEntity,1);
        }

        // initialize the inter-dependency matrix
        int interDependenciesNumberUp = (int) Math.ceil(this.interDependenciesDensityUp*this.callarEntityNumber*this.calleeInterfaceNumber);
        int interDependenciesNumberLow = (int) Math.ceil(this.interDependenciesDensityLow*this.callarEntityNumber*this.calleeInterfaceNumber);
        int interDependenciesNumber = interDependenciesNumberLow;
        if(interDependenciesNumberUp>interDependenciesNumberLow)interDependenciesNumber+=randomSeed.nextInt(interDependenciesNumberUp-interDependenciesNumberLow+1);
        ArrayList<Integer> interDependenciesList = new ArrayList<>();
        for(int k=0;k<interDependenciesNumber;k++){
            int dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
            int dependedInterface = randomSeed.nextInt(this.calleeInterfaceNumber);
            while(interDependenciesList.contains(dependingEntity*this.calleeInterfaceNumber+dependedInterface)){
                dependingEntity = randomSeed.nextInt(this.callarEntityNumber);
                dependedInterface = randomSeed.nextInt(this.calleeInterfaceNumber);
            }
            interDependenciesList.add(dependingEntity*this.calleeInterfaceNumber+dependedInterface);
            this.interDepMatrix.set(dependingEntity,dependedInterface,1);
        }
    }


    /**
     * Step 3: generate operations of interfaces and
     * the directly dependent operations of entities
     */
    private void generateOperationSet(){
        // initialize the operationManage
        this.operationsManage = new OperationsManage(callarEntityNumber,calleeInterfaceNumber);

        // generate random operations of all interfaces
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            this.operationsManage.interfaceOperationSet[i] = this.operationNumberLow+this.randomSeed.nextInt(this.operationNumberUp-this.operationNumberLow+1);
        }

        // generate randomly the dependent operations of each entity
        for(int j=0;j<this.calleeInterfaceNumber;j++){
            // derive the operation number of the current interface
            int operationNumber = this.operationsManage.interfaceOperationSet[j];
            for(int i=0;i<this.callarEntityNumber;i++){
                if(this.interDepMatrix.get(i,j)==1){//the current entity depends on the current interface
                    this.operationsManage.dependentOperationSet[i][j] = new OperationSet(operationNumber);
                    int dependentOperationNumber = randomSeed.nextInt(operationNumber)+1; // at least one operation is depended upon
                    for(int k=0;k<dependentOperationNumber;k++){
                        int currentDependentOperation = randomSeed.nextInt(operationNumber);
                        if(this.operationsManage.dependentOperationSet[i][j].dependentOperations[currentDependentOperation]==1){
                            currentDependentOperation = randomSeed.nextInt(operationNumber);
                        }
                        this.operationsManage.dependentOperationSet[i][j].dependentOperations[currentDependentOperation] = 1;
                    }

                }
            }
        }

    }

    /**
     * Step 4: calculate the coupling values from callar to callee
     */
    private void calculateCouplingValues(){
        System.out.println("calculating ACT");
        this.calculateSimulatedACT();
        System.out.println("calculating CaT");
        this.calculateSimulatedCaT();
        System.out.println("calculating CeT");
        this.calculateSimulatedCeT();
        System.out.println("calculating MCI");
        this.calculateSimulatedMCI();
    }

    /**
     * Step 5: determine whether to delete operations of callee
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
        boolean [] isDependedChangingInterface = new boolean[this.calleeInterfaceNumber];
        boolean isChangingInterfaceDepended = false;
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            if(isChangingInterface[i]){ // an operation deletion event is to be happened
                for(int j=0;j<this.callarEntityNumber;j++){
                    if(this.interDepMatrix.get(j,i)==1){
                        isChangingInterfaceDepended = true;
                        isDependedChangingInterface[i] = true; // the changing interface is depended on
                    }
                }
            }
        }
        if(isChangingInterfaceDepended) // go to step 7
            quantifyDirectRippleEffect(isDependedChangingInterface);
        // else: the current ripple effects are zeros
    }

    /**
     * Step 7: calculate the impacts of deleted operations in callee
     * @param isDependedChangingInterface
     */
    private void quantifyDirectRippleEffect(boolean [] isDependedChangingInterface) {
        this.currentDCF = 0;
        this.directlyAffectedFiles = new HashSet<Integer>();

        for (int i = 0; i < this.calleeInterfaceNumber; i++) {
            if (isDependedChangingInterface[i]) { // determine the ripple effects of the current changing interface (that is depended on by callar)
                // derive the entities that depend on this interface
                ArrayList<Integer> dependingEntity = new ArrayList<Integer>();
                for (int j = 0; j < this.callarEntityNumber; j++) {
                    if (this.interDepMatrix.get(j,i) == 1) {
                        dependingEntity.add(j);
                    }
                }

                // generate the deleted operations
                int operationNumber = this.operationsManage.interfaceOperationSet[i];

                int deletedNumber = 0;
                for (int k = 0; k < operationNumber; k++) {
                    if (randomSeed.nextInt(1000) < this.operationDeletedProbability * 1000) {
                        deletedNumber++;
                        for (int t = 0; t < dependingEntity.size(); t++) { // for each entity that depends on the changing interface
                            if (this.operationsManage.dependentOperationSet[dependingEntity.get(t)][i].dependentOperations[k] == 1) {
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
            this.accumulatedDCF += this.currentDCF;
            this.accumulatedDCS += this.directlyAffectedFiles.size();
            this.quantifyIndirectRippleEffectDFS(this.directlyAffectedFiles);// go to step 8
        }// else: the current direct ripple effects are zeros

    }


    /**
     * Step 8: calculate the indirect impacts of deleted operations in callee
     * @param directlyAffectedFiles
     */
    private void quantifyIndirectRippleEffectDFS(HashSet<Integer> directlyAffectedFiles ) {
        this.indirectlyAffectedFiles = new HashSet<Integer>(); // for recording the indirectly affected files
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

                    for (int j = 0; j < this.callarEntityNumber; j++) {
                        if (this.intraDepMatrix.get(j,file) == 1) rippleStack.push(j);
                    }
                }
            }
        }
        if(this.indirectlyAffectedFiles.size()>this.directlyAffectedFiles.size()){
            System.out.println("something wrong");
        }
        this.accumulatedICF += this.currentICF;
        this.accumulatedICS += this.indirectlyAffectedFiles.size();

        this.accumulatedOCF += (this.currentICF+this.currentDCF);
        this.indirectlyAffectedFiles.addAll(this.directlyAffectedFiles);
        this.accumulatedOCS += this.indirectlyAffectedFiles.size();

    }


    private void updateDistanceTable(int distanceTable[], int entryEntityID){
        int distance = 1 ;
        if(distanceTable[entryEntityID]==-1|| distanceTable[entryEntityID] > distance)
            distanceTable[entryEntityID] = distance;
        putIntraInvokerEntities(distance,distanceTable,entryEntityID);

    }

    private void putIntraInvokerEntities(int distance, int distanceTable[], int curEntityID){
        if(distance == this.callarEntityNumber)return;
        distance++;
        for(int i=0;i<this.callarEntityNumber;i++){
            if(this.intraDepMatrix.get(i,curEntityID)==1){//向上回溯调用了curEntity的callar内实体，并更新其距离表
                if (distanceTable[i]==-1 || distanceTable[i] > distance){
                    distanceTable[i] = distance;
                    putIntraInvokerEntities(distance,distanceTable,i);
                }
            }
        }
    }

    private void calculateSimulatedMCI(){
        int distanceTable[] = new int[this.callarEntityNumber];
        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<this.callarEntityNumber;i++) distanceTable[i] = -1;

        //统计callee微服务中被callar微服务依赖的接口数
        int reachableInterfaces = 0;
        for(int k = 0;k<this.calleeInterfaceNumber;k++){
            boolean reachable = false;
            for(int j=0;j<this.callarEntityNumber;j++){
                if(this.interDepMatrix.get(j,k)==1){
                    reachable=true;
                    // 计算所有微服务2中实体到当前操作的距离并更新distance表
                    updateDistanceTable(distanceTable,j);
                }
            }
            if(reachable)reachableInterfaces++;
        }

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        // 统计依赖微服务中依赖另一个微服务接口的实体数
        for(int i = 0;i<this.callarEntityNumber;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];
            }
        }

        if(reachableEntities!=0 && reachableInterfaces!=0){
            this.simulatedMCI = ((double)reachableInterfaces/this.calleeInterfaceNumber)*((double)reachableEntities/this.callarEntityNumber+(double)reachableEntities/accumulatedDistance);
        }
    }

    private void calculateSimulatedCaT(){
        // calculating the number of entities in the callar microservice depends on the callee
        int dependentEntities = 0;
        for(int i=0;i<this.callarEntityNumber;i++) {
            boolean dependent = false;
            for (int k = 0; k < this.calleeInterfaceNumber; k++) {
                if (this.interDepMatrix.get(i,k)==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentEntities++;
        }
        this.simulatedCaT = dependentEntities;
    }

    private void calculateSimulatedCeT(){
        // calculating the number of interfaces in the callee microservice that is depended on by the callar
        int dependentInterfaces = 0;
        for(int k=0;k<this.calleeInterfaceNumber;k++) {
            boolean dependent = false;
            for (int i = 0; i < this.callarEntityNumber; i++) {
                if (this.interDepMatrix.get(i,k)==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentInterfaces++;
        }
        this.simulatedCeT = dependentInterfaces;
    }

    private void calculateSimulatedACT(){
        // calculating whether the callar microservice depends on the callee
        int dependentMicroservice = 0;
        for(int i=0;i<this.callarEntityNumber;i++) {
            for (int k = 0; k < this.calleeInterfaceNumber; k++) {
                if (this.interDepMatrix.get(i,k)==1){
                    dependentMicroservice=1;break;
                }
            }
        }
        this.simulatedACT = dependentMicroservice;
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
