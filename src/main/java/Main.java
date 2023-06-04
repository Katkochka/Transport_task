import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
//        Matrix boundsMatrix = new Matrix(
//                new DoubleVector(3.0, -1.0),
//                new DoubleVector(1.0, 3.0),
//                new DoubleVector(2.0, 1.0));
//        DoubleVector boundsVector = new DoubleVector(6.0, 12.0, 14.0);
//        DoubleVector pricesVector = new DoubleVector(4.0, -1.0);
//        ArrayList<Sign> inequalities = new ArrayList<>();
//        inequalities.add(Sign.More);
//        inequalities.add(Sign.Less);
//        inequalities.add(Sign.Less);
//
//        SimplexStrelkov simplex = new SimplexStrelkov(boundsMatrix, pricesVector, inequalities, boundsVector);
//        System.out.println(simplex.solve(SimplexProblemType.Max));
//
        TransportTask task = new TransportTask();

//        DoubleVector reserves = new DoubleVector(140.0, 140.0, 120.0);
//        DoubleVector needs = new DoubleVector(115.0, 65.0, 90.0, 130.0);
//        Matrix count = new Matrix(
//                new DoubleVector(1.0, 8.0, 2.0, 9.0),
//                new DoubleVector(8.0, 7.0, 5.0, 1.0),
//                new DoubleVector(5.0, 3.0, 2.0, 4.0)
//        );
//        task.prepareSimplex(needs, reserves, count);
//
//
//        DoubleVector reserves = new DoubleVector(10.0, 5.0);
//        DoubleVector needs = new DoubleVector(6.0, 9.0);
//        Matrix count = new Matrix(
//                new DoubleVector(1.0, 2.0),
//                new DoubleVector(3.0, 1.0)
//        );
//        task.prepareSimplex(needs, reserves, count);
//        task.solve();

        String file = "D:\\Нужное\\Университет\\Методы оптимизации\\lr-3\\src\\main\\resources\\transport_table.xlsx";
        task.prepareSimplex(file);
        task.solve();
    }
}
