import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Simplex {
    private Matrix simplexTable;

    private final Matrix boundsMatrix;
    private final DoubleVector boundsVector;
    private final DoubleVector pricesVector;

    private final List<Sign> inequalities;
    private final List<Integer> fModArgsIndexes;
    private final List<Integer> naturalArgsIndexes;
    private final List<Integer> basisArgsIndexes;

    SimplexProblemType problemType = SimplexProblemType.Max;

    public Simplex(Matrix a, DoubleVector c, List<Sign> inequalities, DoubleVector b) {
        if (b.size() != inequalities.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Error simplex creation:  b.size() ==  %s,inequalities.size() == %s",
                            b.size(),
                            inequalities.size()));
        }

        if (a.rows() != inequalities.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Error simplex creation :: A.rows() == %s, inequalities.size() == %s",
                            a.rows(),
                            inequalities.size()));
        }

        if (a.cols() != c.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Error simplex creation :: A.cols() == %s,  price_coefficients.size() == %s",
                            a.cols(),
                            c.size()));
        }

        this.naturalArgsIndexes = new ArrayList<>();
        this.basisArgsIndexes = new ArrayList<>();
        this.fModArgsIndexes = new ArrayList<>();

        boundsVector = new DoubleVector((DoubleVector) b.clone());
        boundsMatrix = new Matrix((Matrix) a.clone());
        pricesVector = new DoubleVector((DoubleVector) c.clone());
        this.inequalities = inequalities;
    }

    public Simplex(Matrix a, DoubleVector c, DoubleVector b) {
        if (a.rows() != b.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Error simplex creation :: A.rows() == %s, b.size() == %s",
                            a.rows(),
                            b.size()));
        }

        if (a.cols() != c.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Error simplex creation :: A.cols() == %s,  price_coefficients.size() == %s",
                            a.cols(),
                            c.size()));
        }

        inequalities = new ArrayList<>();

        for (int i = 0; i < b.size(); i++) {
            inequalities.add(Sign.Less);
        }

        this.naturalArgsIndexes = new ArrayList<>();
        this.basisArgsIndexes = new ArrayList<>();
        this.fModArgsIndexes = new ArrayList<>();

        boundsVector = new DoubleVector((DoubleVector) b.clone());
        boundsMatrix = new Matrix((Matrix) a.clone());
        pricesVector = new DoubleVector((DoubleVector) c.clone());
    }

    public DoubleVector solve(SimplexProblemType problemType) {
        this.problemType = problemType;
        DoubleVector solution = new DoubleVector(this.naturalArgsN());
        this.buildSimplexTable();

//        this.printSimplex();
        System.out.println(simplexToString());

        double aik;
        int mainRow;
        int mainCol;

        if (excludeModArgs()) {
            System.out.println("Simplex table after args exclude");
        }
//        System.out.println(simplexToString());

        while (!isPlanOptimal()) {
            mainCol = getMainCol();
            if (mainCol == -1) {
                break;
            }

            mainRow = getMainRow(mainCol);
            if (mainRow == -1) {
                System.out.println("Unable to get main row. Simplex is probably boundless...");
                return null;
            }

            basisArgsIndexes.set(mainRow, mainCol);
            aik = simplexTable.get(mainRow, mainCol);
            simplexTable.row(mainRow).mul(1.0 / aik);
            for (int i = 0; i < simplexTable.rows(); i++) {
                if (i == mainRow) {
                    continue;
                }
                simplexTable.row(i).sub(DoubleVector.mul(simplexTable.get(i, mainCol), simplexTable.row(mainRow)));
            }
            solution = this.currentSimplexSolution();
//            this.printSimplex();
//            System.out.println("ABOBA");
            System.out.println(simplexToString());
        }

        if (validateSolution()) {
            solution = currentSimplexSolution(true);
            return solution;
        }

        System.out.println("Simplex is unresolvable");
        return null;
    }

    private void buildSimplexTable() {
        simplexTable = new Matrix(boundsMatrix);
        naturalArgsIndexes.clear();
        basisArgsIndexes.clear();
        fModArgsIndexes.clear();

        for (int row = 0; row < simplexTable.rows(); row++) {
            if (boundsVector.get(row) >= 0) continue;
            inequalities.set(row, inequalities.get(row) == Sign.Less ? Sign.More : Sign.Less);
            boundsVector.set(row, boundsVector.get(row) * -1.0);
            simplexTable.row(row).mul(-1.0);
        }

        for (int i = 0; i < pricesVector.size(); i++) {
            naturalArgsIndexes.add(i);
        }

        int[] basisArgsInfo;
        for (int inequalityId = 0; inequalityId < inequalities.size(); inequalityId++) {
            basisArgsInfo = buildVirtualBasisCol(inequalityId, inequalities.get(inequalityId));

            naturalArgsIndexes.add(basisArgsInfo[0]);

            if (basisArgsInfo[1] != -1) {
                basisArgsIndexes.add(basisArgsInfo[1]);
                fModArgsIndexes.add(basisArgsInfo[1]);
                continue;
            }
            basisArgsIndexes.add(basisArgsInfo[0]);
        }

        for (int row = 0; row < simplexTable.rows(); row++) {
            simplexTable.row(row).pushBack(boundsVector.get(row));
        }

        DoubleVector simDifference = new DoubleVector(simplexTable.cols());
        if (problemType == SimplexProblemType.Max) {
            for (int j = 0; j < simDifference.size(); j++) {
                simDifference.set(j, j < pricesVector.size() ? -pricesVector.get(j) : 0.0);
            }
        } else {
            for (int j = 0; j < simDifference.size(); j++) {
                simDifference.set(j, j < pricesVector.size() ? pricesVector.get(j) : 0.0);
            }
        }

        simplexTable.addRow(simDifference);
        if (!isTargetFuncModified()) {
            return;
        } else {
            DoubleVector sDeltasAdd = new DoubleVector(simplexTable.cols());
            for (Integer fModArgId : fModArgsIndexes) {
                sDeltasAdd.set(fModArgId, 1.0);
            }
            simplexTable.addRow(sDeltasAdd);
        }
    }

    private int[] buildVirtualBasisCol(int inequalityId, Sign inequalitySign) {
        if (inequalitySign == Sign.Equal) {
            return this.buildVirtualBasisColForEqual(inequalityId);
        } else if (inequalitySign == Sign.More) {
            return this.buildVirtualBasisColForMore(inequalityId);
        } else {
            return this.buildVirtualBasisColForLess(inequalityId);
        }
    }

    private int[] buildVirtualBasisColForEqual(int inequalityId) {
        for (int row = 0; row < simplexTable.rows(); row++) {
            if (row == inequalityId) {
                simplexTable.row(row).pushBack(1.0);
                continue;
            }
            simplexTable.row(row).pushBack(0.0);
        }

        return new int[]{simplexTable.cols() - 1, simplexTable.cols() - 1};
    }

    private int[] buildVirtualBasisColForMore(int inequalityId) {
        for (int row = 0; row < simplexTable.rows(); row++) {
            if (row == inequalityId) {
                simplexTable.row(row).pushBack(-1.0);
                simplexTable.row(row).pushBack(1.0);
                continue;
            }

            simplexTable.row(row).pushBack(0.0);
            simplexTable.row(row).pushBack(0.0);
        }

        return new int[]{simplexTable.cols() - 2, simplexTable.cols() - 1};
    }

    private int[] buildVirtualBasisColForLess(int inequalityId) {
        for (int row = 0; row < simplexTable.rows(); row++) {
            if (row == inequalityId) {
                simplexTable.row(row).pushBack(1.0);
                continue;
            }
            simplexTable.row(row).pushBack(0.0);
        }

        return new int[]{simplexTable.cols() - 1, -1};
    }

    private boolean excludeModArgs() {
        if (!isTargetFuncModified()) {
            return false;
        }

        int lastRowIndex = simplexTable.rows() - 1;
        for (int fModArgIndex : this.fModArgsIndexes) {
            for (int row = 0; row < simplexTable.rows(); row++) {
                if (this.simplexTable.get(row, fModArgIndex) == 0) {
                    continue;
                }

                double arg = simplexTable.get(lastRowIndex, fModArgIndex) / simplexTable.get(row, fModArgIndex);
                simplexTable.row(lastRowIndex).sub(DoubleVector.mul(arg, simplexTable.row(row)));
                break;
            }
        }
        return true;
    }

    private int getMainCol() {
        DoubleVector row = this.simplexTable.row(this.simplexTable.rows() - 1);
        double delta = 0;
        int index = -1;

        for (int i = 0; i < row.size() - 1; i++) {
            if (row.get(i) >= delta) {
                continue;
            }
            delta = row.get(i);
            index = i;
        }

        if (!(isTargetFuncModified() && index == -1)) {
            return index;
        }

        row = simplexTable.row(simplexTable.rows() - 2);

        for (int id : naturalArgsIndexes) {
            if (row.get(id) >= delta) {
                continue;
            }
            delta = row.get(id);
            index = id;
        }

        return index;
    }

    private int getMainRow(int simplexCol) {
        double delta = 1e12;
        int index = -1;
        double aik;
        int bIndex = simplexTable.cols() - 1;
        int rowsN = isTargetFuncModified() ? simplexTable.rows() - 2 : simplexTable.rows() - 1;
        for (int i = 0; i < rowsN; i++) {
            aik = simplexTable.get(i, simplexCol);
            if (aik <= 0) {
                continue;
            }

            if (simplexTable.get(i, bIndex) / aik > delta) {
                continue;
            }

            delta = simplexTable.get(i, bIndex) / aik;
            index = i;
        }

        return index;
    }

    private boolean validateSolution() {
        double val = 0;
        int rowsN = isTargetFuncModified() ? simplexTable.rows() - 2 : simplexTable.rows() - 1;
        int colsN = simplexTable.cols() - 1;
        for (int i = 0; i < basisArgsIndexes.size(); i++) {
            if (basisArgsIndexes.get(i) >= naturalArgsN()) {
                continue;
            }

            val += simplexTable.get(i, colsN) * pricesVector.get(basisArgsIndexes.get(i));
        }

        if (problemType == SimplexProblemType.Max) {
            if (Math.abs(val - simplexTable.get(rowsN, colsN)) < 1e-5) {
                if (isTargetFuncModified()) {
                    return (Math.abs(simplexTable.get(simplexTable.rows() - 1, simplexTable.cols() - 1)) < 1e-5);
                }
                return true;
            }
        }

        if (Math.abs(val + simplexTable.get(rowsN, colsN)) < 1e-5) {
            if (isTargetFuncModified()) {
                return (Math.abs(simplexTable.get(simplexTable.rows() - 1, simplexTable.cols() - 1)) < 1e-5);
            }
            return true;
        }
        return false;
    }

    private void printSimplex() {
        List<Integer> basisArgsIndexesCopy = new ArrayList<>(this.basisArgsIndexes);
        Collections.sort(basisArgsIndexesCopy);

        System.out.print("\t\t\t");
        for (int i = 0; i < this.simplexTable.cols() - 1; i++) {
            System.out.print(String.format("X%s\t\t\t", i + 1));
        }
        System.out.println("b");
        for (int i = 0; i < this.simplexTable.rows(); i++) {
            if (i < basisArgsIndexesCopy.size()) {
                System.out.print(String.format("X%s\t\t", basisArgsIndexesCopy.get(i) + 1));
            } else {
                System.out.print("d\t\t");
            }
            for (int j = 0; j < simplexTable.cols(); j++) {
                System.out.print(String.format("%.3f\t\t", this.simplexTable.get(i, j)));
            }
            System.out.println();
        }

        System.out.print("Basis: ");
        for (int xIndex : basisArgsIndexesCopy) {
            System.out.print(String.format("X%s = %f ", xIndex + 1, this.currentSimplexSolution().get(xIndex)));
        }
        System.out.println("\n\n");
    }

    public boolean isTargetFuncModified() {
        return fModArgsIndexes.size() != 0;
    }

    public int naturalArgsN() {
        return pricesVector.size();
    }

    public boolean isPlanOptimal() {
        DoubleVector row = simplexTable.row(simplexTable.rows() - 1);
        boolean res = true;
        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) >= 0) {
                continue;
            }
            res = false;
            break;
        }

        if (isTargetFuncModified()) {
            if (!res) {
                return res;
            }
            DoubleVector penultimateRow = simplexTable.row(simplexTable.rows() - 2);
            for (int index : this.naturalArgsIndexes) {
                if (penultimateRow.get(index) >= 0) {
                    continue;
                }
                res = false;
                break;
            }
        }
        return res;
    }

    public DoubleVector currentSimplexSolution() {
        return this.currentSimplexSolution(false);
    }

    public DoubleVector currentSimplexSolution(boolean onlyNaturalArgs) {
        DoubleVector solution = new DoubleVector(onlyNaturalArgs ? naturalArgsN() : simplexTable.cols() - 1);
        for (int i = 0; i < basisArgsIndexes.size(); i++) {
            if (basisArgsIndexes.get(i) >= solution.size()) {
                continue;
            }

            solution.set(basisArgsIndexes.get(i), simplexTable.get(i, simplexTable.cols() - 1));
        }
        return solution;
    }

    public String simplexToString() {
        if (simplexTable.rows() == 0) return "";

        StringBuilder sb = new StringBuilder();

        int i = 0;

        sb.append(String.format("%-6s", " "));

        for (; i < simplexTable.cols() - 1; i++) {
            sb.append(String.format("|%-12s", " x " + String.valueOf((i + 1))));
        }
        sb.append(String.format("|%-12s", " b"));

        sb.append("\n");

        int nRow = -1;

        for (DoubleVector row : simplexTable.getRows()) {
            nRow++;

            if (isTargetFuncModified()) {
                if (nRow == simplexTable.rows() - 2) {
                    sb.append(String.format("%-6s", " d0"));
                } else if (nRow == simplexTable.rows() - 1) {
                    sb.append(String.format("%-6s", " d1"));
                } else {
                    sb.append(String.format("%-6s", " x " + String.valueOf(basisArgsIndexes.get(nRow) + 1)));
                }
            } else {
                if (nRow == simplexTable.rows() - 1) {
                    sb.append(String.format("%-6s", " d"));
                } else {
                    sb.append(String.format("%-6s", " x " + String.valueOf(basisArgsIndexes.get(nRow) + 1)));
                }
            }

            for (int col = 0; col < row.size(); col++) {
                if (row.get(col) >= 0) {
                    sb.append(String.format("|%-12s", " " + NumericUtils.toRationalStr(row.get(col))));
                    continue;
                }
                sb.append(String.format("|%-12s", NumericUtils.toRationalStr(row.get(col))));

            }
            sb.append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }
}
