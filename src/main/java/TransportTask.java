import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransportTask {
    private Simplex simplex;
    private DoubleVector pricesVector;
    private DoubleVector boundsVector;
    private Matrix simplexMatrix;
    private ArrayList<Sign> inequalities;

    public void prepareSimplex(DoubleVector needsVector, DoubleVector reservesVector, Matrix countMatrix) {
        this.inequalities = new ArrayList<>();
        for (int i = 0; i < reservesVector.size(); i++) {
            inequalities.add(Sign.Equal);
        }
        this.simplex = new Simplex(countMatrix, needsVector, inequalities, reservesVector);
    }

    public void prepareSimplex(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        XSSFWorkbook workbook =  new XSSFWorkbook(fis);
        XSSFSheet sheet = workbook.getSheetAt(0);

        Iterator<Row> rowIterator = sheet.iterator();
        int whatIsReading = 0;

        DoubleVector reserves = null;
        DoubleVector needs = null;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            Iterator<Cell> cellIterator = row.iterator();
            if (row.getCell(0).getCellType() == CellType.NUMERIC) {
                List<Double> dataList = new ArrayList<>();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    dataList.add(cell.getNumericCellValue());
                }

                DoubleVector dataVector = new DoubleVector(dataList);

                if (whatIsReading == 0) {
                    if (this.simplexMatrix == null) {
                        this.simplexMatrix = new Matrix(dataVector);
                    } else {
                        this.simplexMatrix.addRow(dataVector);
                    }
                } else if (whatIsReading == 1) {
                    reserves = dataVector;
                } else if (whatIsReading == 2) {
                    needs = dataVector;
                } else if (whatIsReading == 3) {
                    this.pricesVector = dataVector;
                }
            } else {
                whatIsReading++;
            }
        }

        List<Double> boundsList = new ArrayList<>();
        this.inequalities = new ArrayList<>();
        for (int i = 0; i < reserves.size(); i++) {
            boundsList.add(reserves.get(i));
            inequalities.add(Sign.Less);
        }

        for (int i = 0; i < needs.size(); i++) {
            boundsList.add(needs.get(i));
            inequalities.add(Sign.More);
        }

        this.boundsVector = new DoubleVector(boundsList);

        System.out.println(simplexMatrix);
        System.out.println(boundsVector);
        System.out.println(pricesVector);

        this.simplex = new Simplex(simplexMatrix, pricesVector, inequalities, boundsVector);

        fis.close();
    }

    public void solve() throws InterruptedException {
        System.out.println(this.simplex.solve(SimplexProblemType.Min));
    }
}
