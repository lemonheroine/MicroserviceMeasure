import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.IOException;
import java.util.HashSet;

public class DPC {

    int microservicesCount;
    int entitiesCount;
    int interfacesCount;
    HashSet<DependenceMatrix> distinctDepMatrix;

    public static void main(String[] args) throws IOException {
        new DPC(10,2,1).calculateMetricsDPC();
    }

    private DPC(int microservicesCount,int entitiesCount,int interfacesCount){
        this.microservicesCount = microservicesCount;
        this.entitiesCount = entitiesCount;
        this.interfacesCount = interfacesCount;
    }


    public void calculateMetricsDPC(){
        calculateMetricsDPC2Microservice();
        calculateMetricsDPC4Microservice();
    }

    // the DPC of metrics that measure the afferent coupling
    public void calculateMetricsDPC2Microservice(){
        distinctDepMatrix = new HashSet<DependenceMatrix>();
        HashSet<Double> distinctMeasurements = new HashSet<Double>();
        DependenceMatrix curDepMatrix =new DependenceMatrix();

        tryPutMatrixIntoSet(curDepMatrix);
        derivePossibleMatrix(curDepMatrix,0,0);

        System.out.println(distinctDepMatrix.size());
    }

    public void derivePossibleMatrix(DependenceMatrix curDepMatrix,int i,int j){
        DependenceMatrix temp = null;
        if(j<entitiesCount && i == j);//一个微服务内实体与自身的依赖关系不为1
        else {
            temp = new DependenceMatrix(curDepMatrix);
            temp.microservicesDepMatrix[i][j] =1;
            tryPutMatrixIntoSet(temp);
        }

        if(i<entitiesCount-1) {
            if(temp != null)derivePossibleMatrix(new DependenceMatrix(temp),i+1,j);
            derivePossibleMatrix(new DependenceMatrix(curDepMatrix),i+1,j);
        }

        if(j<entitiesCount+interfacesCount-1){
            if(temp != null)derivePossibleMatrix(new DependenceMatrix(temp),i,j+1);
            derivePossibleMatrix(new DependenceMatrix(curDepMatrix),i,j+1);
        }
    }

    public void tryPutMatrixIntoSet(DependenceMatrix curDepMatrix){
        int isExist[] = new int[1];
        if(distinctDepMatrix.contains(curDepMatrix)){//no transformation
            isExist[0]=1;return;
        }

    }

    public void isMatrixExist(DependenceMatrix curDepMatrix, int isExist[], int mode, int index){
        if(distinctDepMatrix.contains(curDepMatrix)){//no transformation
            isExist[0]=1;return;
        }

    }

    public void isMatrixExistRowPermutation(DependenceMatrix curDepMatrix, int isExist[],int fixedIndex){
        if(entitiesCount<=1)return;

        isMatrixExistRowPermutation(curDepMatrix,isExist,fixedIndex+1);
        int length = entitiesCount-fixedIndex;
        for(int j = 0;j<length;j++){
            DependenceMatrix transformedMatrix = matrixColumnTransform(curDepMatrix,fixedIndex,j);
            if(distinctDepMatrix.contains(transformedMatrix)){
                isExist[0] =1;return;
            }
        }
    }


    // the DPC of metrics that measure the efferent coupling
    public void calculateMetricsDPC4Microservice(){

    }

    public DependenceMatrix matrixLowTransform(DependenceMatrix matrix, int i, int j){
        DependenceMatrix temp = new DependenceMatrix(matrix);
        for (int k = 0; k < entitiesCount+interfacesCount; k++){
            temp.microservicesDepMatrix[i][k] = matrix.microservicesDepMatrix[j][k];
            temp.microservicesDepMatrix[j][k] = matrix.microservicesDepMatrix[i][k];
        }
        return temp;
    }

    public DependenceMatrix matrixColumnTransform(DependenceMatrix matrix, int i, int j){
        DependenceMatrix temp = new DependenceMatrix(matrix);
        for (int k = 0; k < entitiesCount; k++){
            temp.microservicesDepMatrix[k][i] = matrix.microservicesDepMatrix[k][j];
            temp.microservicesDepMatrix[k][j] = matrix.microservicesDepMatrix[k][i];
        }
        return temp;
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
    }

}
