import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

public class SimulationAfferent {
    // input
    private ArrayList<Integer> callerEntityNumbers;
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
    private CalleeOperations calleeOperations;
    private int callerNumber;

    Random randomSeed;

    // results
    private double[] simulatedAIS;
    private double[] simulatedCa;
    private double[] simulatedMCIor;

    private double simulatedAISValue;
    private double simulatedCaValue;
    private double simulatedMCIorValue;


    private double accumulatedDCFValue;
    private double accumulatedDCSValue;
    private double accumulatedICFValue;
    private double accumulatedICSValue;
    private double accumulatedOCFValue;
    private double accumulatedOCSValue;

    private double[] accumulatedDCF;
    private double[] accumulatedDCS;
    private double[] accumulatedICF;
    private double[] accumulatedICS;
    private double[] accumulatedOCF;
    private double[] accumulatedOCS;

    private int[] Depi;
    private int depe;
    private int accumulatedDistance;
    private int accumulatedEntityNumber;


    private int currentDCF;
    private HashSet<Integer> directlyAffectedFiles;
    private int currentICF;
    private HashSet<Integer> indirectlyAffectedFiles;

    /**
     * Step 1: get input

     */
    private SimulationAfferent(int callerNumber, int operationNumberLow, int operationNumberUp,
                               double interDependenciesDensityLow, double interDependenciesDensityUp, double intraDependenciesDensityLow, double intraDependenciesDensityUp, int iterations,
                               double interfaceChangingProbability, double operationDeletedProbability, double indirectPropagationProbability){
        this.callerNumber = callerNumber;
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
        // test
        this.depe =0;
        this.accumulatedDistance = 0;
        this.accumulatedEntityNumber = 0;
        this.Depi = new int[this.calleeInterfaceNumber];

        System.out.println("calculating coupling");
        this.calculateCouplingValues(); //step 4

        this.accumulatedDCF = new double[this.callerNumber];
        this.accumulatedDCS = new double[this.callerNumber];
        this.accumulatedICF = new double[this.callerNumber];
        this.accumulatedICS = new double[this.callerNumber];
        this.accumulatedOCF = new double[this.callerNumber];
        this.accumulatedOCS = new double[this.callerNumber];
        this.accumulatedDCFValue = 0;
        this.accumulatedDCSValue = 0;
        this.accumulatedICFValue = 0;
        this.accumulatedICSValue = 0;
        this.accumulatedOCFValue = 0;
        this.accumulatedOCSValue = 0;



        int remainTimes = this.iterations;
        while(remainTimes-->0){ // iteration times for step 5 to step 8
            this.changeCalleeMicroservice();
        }

        printSimulationResult(sheet,rowIndex);
    }

    private void printSimulationResult(Sheet sheet, int rowIndex){
        int overallEntities = 0;
        for(int i=0;i<this.callerNumber;i++){
            overallEntities += this.callerEntityNumbers.get(i);

        }
        System.out.println("overallEntities: "+overallEntities);
        double base = this.iterations*overallEntities;

        Util.writeToSimulationAfferentResult(sheet,rowIndex,this.calleeInterfaceNumber,
                this.simulatedAISValue,this.simulatedCaValue,this.simulatedMCIorValue,
                (double)this.accumulatedDCFValue/base,(double)this.accumulatedDCSValue/base,
                (double)this.accumulatedICFValue/base, (double)this.accumulatedICSValue/base,
                (double)this.accumulatedOCFValue/base,(double)this.accumulatedOCSValue/base);

    }

    public static void main(String[] args) throws IOException {
        String resultPrefix = "data/simulation_RQ301/";

        Workbook workbook  = new XSSFWorkbook();
        Sheet sheet = Util.createSimulationAfferentResult(workbook,"round1");
        int rowIndex = 0;
        int resultNumber = 100;
        while(resultNumber-->0){
            SimulationAfferent simulation = new SimulationAfferent(10,
                    3,7,0,1,
                    0,1,1000,
                    0.02,0.33,0.02);
            simulation.simulatingChanges(sheet,rowIndex++);
        }

        Util.writeExcel(workbook,resultPrefix+"simulationAfferentResult"+"RandomSize.xlsx");

    }

    /**
     * Step 2: generate a random matrix
     */
    private void generateDepMatrix(){
        this.interDepMatrices = new ArrayList<>();
        this.intraDepMatrices = new ArrayList<>();
        // initialize the size of callee
//        this.calleeInterfaceNumber = randomSeed.nextInt(4)+1; // the interface range of callee is 1-4
        this.calleeInterfaceNumber = 4;
        // initialize the sizes of caller and the dependency matrices
        this.callerEntityNumbers = new ArrayList<>();
        for (int i=0;i<this.callerNumber;i++){
            int callerEntityNumber = randomSeed.nextInt(25)+4; // the entity range of caller is 4-28
            this.callerEntityNumbers.add(callerEntityNumber);

            this.interDepMatrices.add(new InterDepMatrix(callerEntityNumber,this.calleeInterfaceNumber));
            this.intraDepMatrices.add(new IntraDepMatrix(callerEntityNumber));

            // initialize the intra-dependency matrix
            int intraDependenciesNumberUp = (int) Math.ceil(this.intraDependenciesDensityUp*callerEntityNumber*(callerEntityNumber-1));
            int intraDependenciesNumberLow = (int) Math.floor(this.intraDependenciesDensityLow*callerEntityNumber*(callerEntityNumber-1));
            int intraDependenciesNumber = intraDependenciesNumberLow;
            if(intraDependenciesNumberUp>intraDependenciesNumberLow)intraDependenciesNumber+=randomSeed.nextInt(intraDependenciesNumberUp-intraDependenciesNumberLow+1);
            ArrayList<Integer> intraDependenciesList = new ArrayList<>();
            for(int k=0;k<intraDependenciesNumber;k++){
                int dependingEntity = randomSeed.nextInt(callerEntityNumber);
                int dependedEntity = randomSeed.nextInt(callerEntityNumber);
                while(intraDependenciesList.contains(dependingEntity*callerEntityNumber+dependedEntity)
                        ||dependingEntity==dependedEntity){
                    dependingEntity = randomSeed.nextInt(callerEntityNumber);
                    dependedEntity = randomSeed.nextInt(callerEntityNumber);
                }
                intraDependenciesList.add(dependingEntity*callerEntityNumber+dependedEntity);
                this.intraDepMatrices.get(i).set(dependingEntity,dependedEntity,1);
            }

            // initialize the inter-dependency matrix
            int interDependenciesNumberUp = (int) Math.ceil(this.interDependenciesDensityUp*callerEntityNumber*this.calleeInterfaceNumber);
            int interDependenciesNumberLow = (int) Math.ceil(this.interDependenciesDensityLow*callerEntityNumber*this.calleeInterfaceNumber);
            int interDependenciesNumber = interDependenciesNumberLow;
            if(interDependenciesNumberUp>interDependenciesNumberLow)interDependenciesNumber+=randomSeed.nextInt(interDependenciesNumberUp-interDependenciesNumberLow+1);
            ArrayList<Integer> interDependenciesList = new ArrayList<>();
            for(int k=0;k<interDependenciesNumber;k++){
                int dependingEntity = randomSeed.nextInt(callerEntityNumber);
                int dependedInterface = randomSeed.nextInt(this.calleeInterfaceNumber);
                while(interDependenciesList.contains(dependingEntity*this.calleeInterfaceNumber+dependedInterface)){
                    dependingEntity = randomSeed.nextInt(callerEntityNumber);
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
        this.calleeOperations = new CalleeOperations(this.calleeInterfaceNumber);
        // generate random operations of all interfaces
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            this.calleeOperations.interfaceOperationSet[i] = this.operationNumberLow+this.randomSeed.nextInt(this.operationNumberUp-this.operationNumberLow+1);
        }

        // generate random dependent operations of the entities in each caller service
        this.operationsManages = new ArrayList<>();
        for (int ith=0;ith<this.callerNumber;ith++){
            int callerEntityNumber = this.callerEntityNumbers.get(ith);

            // initialize the operationManage
            this.operationsManages.add(new OperationsManage(callerEntityNumber,this.calleeInterfaceNumber));

            // generate randomly the dependent operations of each entity
            for(int j=0;j<this.calleeInterfaceNumber;j++){
                // derive the operation number of the current interface
                int operationNumber = this.calleeOperations.interfaceOperationSet[j];

                for(int i=0;i<callerEntityNumber;i++){
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
     * Step 4: calculate the coupling values from caller to callee
     */
    private void calculateCouplingValues(){
        this.simulatedAIS = new double[this.callerNumber];
        this.simulatedCa = new double[this.callerNumber];
        this.simulatedMCIor = new double[this.callerNumber];


        for(int ith=0;ith<this.callerNumber;ith++){
            double AIS = this.calculateSimulatedACT(ith);
            this.simulatedAIS[ith] = AIS;
            this.simulatedAISValue += AIS;
            double Ca = this.calculateSimulatedCaT(ith);
            this.simulatedCa[ith] = Ca;
            this.simulatedCaValue += Ca;
            double MCI = this.calculateSimulatedMCI(ith);
            this.simulatedMCIor[ith] = MCI;

        }

        int reachableInterfaces = 0;
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            reachableInterfaces+=this.Depi[i];
        }

        if(reachableInterfaces!=0){
            this.simulatedMCIorValue = ((double) reachableInterfaces / (this.calleeInterfaceNumber)) *
                    ((double) this.depe / this.accumulatedEntityNumber + (double) this.depe / this.accumulatedDistance);
        }


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
        if(isChangingInterfaceInCallee) {
            deleteOperationsOfCallee(isChangingInterface);
            checkRippleEffect(isChangingInterface);// go to step 6
        }// else: the current ripple effects are zeros

    }

    /**
     * Step 5.1: determine whether to delete operations of callee
     */
    private void deleteOperationsOfCallee(boolean [] isChangingInterface){
        this.calleeOperations.initializeOperationChangingSet();
        for(int i=0;i<this.calleeInterfaceNumber;i++){
            if(isChangingInterface[i]){// an interface changing event is to be happened
                int operationNumber = this.calleeOperations.interfaceOperationSet[i];
                // for each operation, determine whether it will be deleted
                for(int j=0;j<operationNumber;j++){
                    if (randomSeed.nextInt(1000) < this.operationDeletedProbability * 1000) {
                        this.calleeOperations.interfaceOperationChangingSet[i].operations[j] = 1;
                    }
                }
            }
        }
    }

    /**
     * Step 6: determine whether there is entity in caller invoking the interface deleting operations
     * @param isChangingInterface
     */
    private void checkRippleEffect(boolean [] isChangingInterface){
        for(int ith=0;ith<this.callerNumber;ith++){
            int callerEntityNumber = this.callerEntityNumbers.get(ith);

            boolean [] isDependedChangingInterface = new boolean[this.calleeInterfaceNumber];
            boolean isChangingInterfaceDepended = false;

            for(int i=0;i<this.calleeInterfaceNumber;i++){
                if(isChangingInterface[i]){ // an operation deletion event is to be happened
                    for(int j=0;j<callerEntityNumber;j++){
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
        int callerEntityNumber = this.callerEntityNumbers.get(ith);

        for (int i = 0; i < this.calleeInterfaceNumber; i++) {
            if (isDependedChangingInterface[i]) { // determine the ripple effects of the current changing interface (that is depended on by caller)
                // derive the entities that depend on this interface
                ArrayList<Integer> dependingEntity = new ArrayList<Integer>();
                for (int j = 0; j < callerEntityNumber; j++) {
                    if (this.interDepMatrices.get(ith).get(j,i) == 1) {
                        dependingEntity.add(j);
                    }
                }

                // generate the deleted operations
                int operationNumber = this.calleeOperations.interfaceOperationSet[i];

                for (int k = 0; k < operationNumber; k++) {
                    if(this.calleeOperations.interfaceOperationChangingSet[i].operations[k]==1){ // the current operation is deleted
                        for (int t = 0; t < dependingEntity.size(); t++) { // for each entity that depends on the changing interface
                            if (this.operationsManages.get(ith).dependentOperationSet[dependingEntity.get(t)][i].operations[k] == 1) {
                                    // the current entity depends on the deleted operation
                                    this.currentDCF++;
                                    this.directlyAffectedFiles.add(dependingEntity.get(t));
                            }

                        }
                    }
                }
            }

        }

        if (this.currentDCF != 0) {
            this.accumulatedDCF[ith] += (double)this.currentDCF/callerEntityNumber;
            this.accumulatedDCS[ith] += (double)this.directlyAffectedFiles.size()/callerEntityNumber;
            this.accumulatedDCFValue += this.currentDCF;
            this.accumulatedDCSValue += this.directlyAffectedFiles.size();
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
        int callerEntityNumber = this.callerEntityNumbers.get(ith);

        for (Integer directlyAffectedFile : directlyAffectedFiles) {
            Stack<Integer> rippleStack = new Stack<>();
            boolean[] fileAffected = new boolean[callerEntityNumber];
            fileAffected[directlyAffectedFile] = true;

            for (int i = 0; i < callerEntityNumber; i++)
                if (this.intraDepMatrices.get(ith).get(i,directlyAffectedFile) == 1) rippleStack.push(i);

            while (!rippleStack.isEmpty()) {
                Integer file = rippleStack.pop();

                int changeRandom = randomSeed.nextInt(1000);
                if (changeRandom< this.indirectPropagationProbability * 1000
                        && fileAffected[file] == false) {
                    this.indirectlyAffectedFiles.add(file);
                    fileAffected[file] = true;
                    this.currentICF++;

                    for (int j = 0; j < callerEntityNumber; j++) {
                        if (this.intraDepMatrices.get(ith).get(j,file) == 1) rippleStack.push(j);
                    }
                }
            }
        }
        if(this.indirectlyAffectedFiles.size()>this.directlyAffectedFiles.size()){
//            System.out.println("the indirect ripple effects are larger than the direct");
        }
        this.accumulatedICF[ith] += (double)this.currentICF/callerEntityNumber;
        this.accumulatedICS[ith] += (double)this.indirectlyAffectedFiles.size()/callerEntityNumber;
        this.accumulatedICFValue += this.currentICF;
        this.accumulatedICSValue += this.indirectlyAffectedFiles.size();

        this.accumulatedOCF[ith] += (double)(this.currentICF+this.currentDCF)/callerEntityNumber;
        this.indirectlyAffectedFiles.addAll(this.directlyAffectedFiles);
        this.accumulatedOCS[ith] += (double)this.indirectlyAffectedFiles.size()/callerEntityNumber;

        this.accumulatedOCFValue += (this.currentICF+this.currentDCF);
        this.accumulatedOCSValue += this.indirectlyAffectedFiles.size();
    }


    private void updateDistanceTable(int ith, int distanceTable[], int entryEntityID){
        int distance = 1 ;
        if(distanceTable[entryEntityID]==-1|| distanceTable[entryEntityID] > distance)
            distanceTable[entryEntityID] = distance;
        putIntraInvokerEntities(ith,distance,distanceTable,entryEntityID);

    }

    private void putIntraInvokerEntities(int ith, int distance, int distanceTable[], int curEntityID){
        int callerEntityNumber = this.callerEntityNumbers.get(ith);
        if(distance == callerEntityNumber)return;
        distance++;
        for(int i=0;i<callerEntityNumber;i++){
            if(this.intraDepMatrices.get(ith).get(i,curEntityID)==1){//向上回溯调用了curEntity的caller内实体，并更新其距离表
                if (distanceTable[i]==-1 || distanceTable[i] > distance){
                    distanceTable[i] = distance;
                    putIntraInvokerEntities(ith,distance,distanceTable,i);
                }
            }
        }
    }

    private double calculateSimulatedMCI(int ith){
        int callerEntityNumber = this.callerEntityNumbers.get(ith);
        int distanceTable[] = new int[callerEntityNumber];
        // 初始化所有微服务2中实体到微服务1中接口的距离为-1
        for(int i=0;i<callerEntityNumber;i++) distanceTable[i] = -1;

        //统计callee微服务中被caller微服务依赖的接口数
        int reachableInterfaces = 0;
        for(int k = 0;k<this.calleeInterfaceNumber;k++){
            boolean reachable = false;
            for(int j=0;j<callerEntityNumber;j++){
                if(this.interDepMatrices.get(ith).get(j,k)==1){
                    this.Depi[k]=1;
                    reachable=true;
                    // 计算所有微服务2中实体到当前操作的距离并更新distance表
                    updateDistanceTable(ith,distanceTable,j);
                }
            }
            if(reachable)reachableInterfaces++;
        }

//        this.depi += reachableInterfaces;

        int reachableEntities = 0;
        int accumulatedDistance = 0;
        // 统计依赖微服务中依赖另一个微服务接口的实体数
        for(int i = 0;i<callerEntityNumber;i++){
            if (distanceTable[i]!=-1){
                reachableEntities++;
                accumulatedDistance += distanceTable[i];
            }
        }

        // test
        this.depe += reachableEntities;
        this.accumulatedDistance += accumulatedDistance;
        this.accumulatedEntityNumber += callerEntityNumber;

        if(reachableEntities!=0 && reachableInterfaces!=0) {
            return ((double) reachableInterfaces / this.calleeInterfaceNumber) * ((double) reachableEntities / callerEntityNumber + (double) reachableEntities / accumulatedDistance);
        }
        return 0;
    }

    private double calculateSimulatedCaT(int ith){
        // calculating the number of entities in the caller microservice depends on the callee
        int dependentEntities = 0;
        int callerEntityNumber = this.callerEntityNumbers.get(ith);
        for(int i=0;i<callerEntityNumber;i++) {
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
        // calculating the number of interfaces in the callee microservice that is depended on by the caller
        int dependentInterfaces = 0;
        int callerEntityNumber = this.callerEntityNumbers.get(ith);
        for(int k=0;k<this.calleeInterfaceNumber;k++) {
            boolean dependent = false;
            for (int i = 0; i < callerEntityNumber; i++) {
                if (this.interDepMatrices.get(ith).get(i,k)==1){
                    dependent=true;break;
                }
            }
            if(dependent)dependentInterfaces++;
        }
        return dependentInterfaces;
    }

    private double calculateSimulatedACT(int ith){
        // calculating whether the caller microservice depends on the callee
        int dependentMicroservice = 0;
        int callerEntityNumber = this.callerEntityNumbers.get(ith);
        for(int i=0;i<callerEntityNumber;i++) {
            for (int k = 0; k < this.calleeInterfaceNumber; k++) {
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

    class CalleeOperations{
        int interfaceNumber;
        int[] interfaceOperationSet;
        OperationSet[] interfaceOperationChangingSet;
        private CalleeOperations(int interfaceNumber){
            this.interfaceNumber = interfaceNumber;
            this.interfaceOperationSet = new int[interfaceNumber];
            this.interfaceOperationChangingSet = new OperationSet[interfaceNumber];
        }
        private void initializeOperationChangingSet(){
            for(int i=0;i<this.interfaceNumber;i++){
                this.interfaceOperationChangingSet[i] =new OperationSet(this.interfaceOperationSet[i]);
            }
        }
    }

    class OperationsManage{
        OperationSet[][] dependentOperationSet; //of per entity and per interface

        private OperationsManage(int entityNumber, int interfaceNumber){
            this.dependentOperationSet = new OperationSet[entityNumber][interfaceNumber];
        }

    }

}
