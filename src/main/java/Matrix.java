public class Matrix extends TemplateVector<DoubleVector>
{
    public static  boolean showMatrixDebugLog = false;

    public Matrix(DoubleVector...  rows)
    {
        super();

        if (rows == null) throw new RuntimeException("Data is null...");

        if (rows.length == 0) throw new RuntimeException("Data is empty...");

        int rowSizeMax = Integer.MIN_VALUE;

        int rowSizeMin = Integer.MAX_VALUE;

        for(DoubleVector row:rows)
        {
            if(row.size() > rowSizeMax) rowSizeMax = row.size();
            if(row.size() < rowSizeMin) rowSizeMin = row.size();
        }

        if(rowSizeMax!=rowSizeMin) throw new RuntimeException("Incorrect matrix data");

        for (DoubleVector v: rows) pushBack((DoubleVector) v.clone());
    }

    /**
     * Конструктор матрцы по ее размерам и элементу по умолчанию.
     * @param n_rows колическтво строк.
     * @param n_cols количество столбцов.
     */
    public Matrix(int n_rows, int n_cols)
    {
        super(n_rows);
        for (int i = 0; i < n_rows; i++) set(i, new DoubleVector(n_cols));
    }

    /**
     * Конструктор копирования.
     * @param original исходная матрица.
     */
    public Matrix(Matrix original)
    {
        super();
        for (DoubleVector v: original)pushBack((DoubleVector) v.clone());
    }

    @Override
    public String toString() {
        StringBuilder sb =new StringBuilder();
        sb.append("{\n");
        for (DoubleVector row : getRows())
        {
            sb.append(" ").append(row.toString());
            sb.append(";\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Строка матрицы.
     * @param rowId номер строки.
     * @return строка матрицы.
     */
    public DoubleVector row(int rowId)
    {
        return get(rowId);
    }

    /**
     * Вектор строк матрицы.
     * @return вектор строк матрицы.
     */
    public TemplateVector<DoubleVector> getRows()
    {
        return this;
    }

    /**
     * Количество строк.
     * @return Количество строк.
     */
    public int rows()
    {
        return super.size();
    }

    /**
     * Количество столбцов.
     * @return  Количество столбцов.
     */
    public int cols()
    {
        if (rows() == 0) return 0;
        return row(0).size();
    }

    /**
     * Добавляет новый столбец к матрице.
     * @param col новый стобец.
     * @return обновлённая матрица.
     */
    public Matrix addCol(DoubleVector col)
    {
        if (col == null) return this;
        if (col.size() != rows()) throw new RuntimeException("Error::AddCol::col.Size != NRows");
        int index = 0;
        for (DoubleVector row: this)
        {
            row.pushBack(col.get(index));
            index++;
        }
        return this;
    }

    /**
     * Добавляет новую строку к матрице.
     * @param row новая строка.
     * @return обновлённая матрица.
     */
    public Matrix addRow(DoubleVector row)
    {
        if(row == null) return this;
        if (row.size() != cols()) throw new RuntimeException("Error::AddRow::row.Size != NCols");
        pushBack(row);
        return this;
    }

    /**
     *  Размерность матрицы.
     * @return массив из целых чисел (количество строк, количество столбцов).
     */
    public final int[] shape()
    {
        return new int[] { rows(), cols() };
    }

    /**
     * Элемент матрицы
     * @param row номер строки
     * @param col номер столбца
     * @return элемент матрицы[row, col]
     */
    public double get(int row, int col)
    {
        return get(row).get(col);
    }

    /**
     * Устанавливает новое значение элемента матрицы
     * элемент матрицы[row, col] = value
     * @param row номер строки
     * @param col номер столбца
     * @param value новое значение элемента матрицы
     * @return обновлённая матрица.
     */
    public Matrix set(int row, int col, double value)
    {
        get(row).set(col, value);
        return this;
    }

    public static Matrix hessian(IFunctionND f, DoubleVector x, double eps)
    {
        Matrix res = new Matrix(x.size(), x.size());
        int row, col;
        double val;
        for (row = 0; row < res.rows(); row++)
        {
            for (col = 0; col <= row; col++)
            {
                val = DoubleVector.partial2(f, x, row, col, eps);
                res.set(row,col,val);
                res.set(col,row,val);
            }
        }
        return res;
    }

    public static Matrix hessian(IFunctionND f, DoubleVector x)
    {
        return hessian( f,  x, 1e-5);
    }

    public static int rank(Matrix A)
    {
        int n = A.rows();

        int m = A.cols();

        int rank = 0;

        boolean[] row_selected = new boolean[n];

        for (int i = 0; i < m; i++)
        {
            int j;

            for (j = 0; j < n; j++) if (!row_selected[j] && Math.abs(A.get(i,j)) > 1e-12) break;

            if (j != n)
            {
                ++rank;

                row_selected[j] = true;

                for (int p = i + 1; p < m; p++) A.set(j, p, A.get(j,p) / A.get(j, i));

                for (int k = 0; k < n; k++)
                {
                    if (k != j && Math.abs(A.get(k,i)) > 1e-12)
                    {
                        for (int p = i + 1; p < m; p++) A.set(k,p,A.get(k,p) - A.get(j,p) * A.get(k,i));
                    }
                }
            }
        }
        return rank;
    }

    /**
     * Проверяет совместность СЛАУ вида Ax = b. Используется теорема Кронекера-Капелли
     * @param A matrix
     * @param b vector
     * @return 0 - нет решений, 1 - одно решение, 2 - бесконечное множествое решений
     */
    public static SolutionType checkSystem(Matrix A, DoubleVector b)
    {
        Matrix a = new Matrix(A);

        int rank_a = Matrix.rank(a);

        Matrix ab = new Matrix(A);

        int rank_a_b = Matrix.rank(ab.addCol(b));

        if(showMatrixDebugLog)
        {
            System.out.println("rank ( A ) " + rank_a + "\n");

            System.out.println("rank (A|b) " + rank_a_b + "\n");
            if (rank_a == rank_a_b) System.out.println("one solution\n");

            if (rank_a < rank_a_b) System.out.println("infinite amount of solutions\n");

            if (rank_a > rank_a_b) System.out.println("no solutions\n");
        }

        if (rank_a == rank_a_b)  return SolutionType.Single;

        if (rank_a < rank_a_b) return SolutionType.Infinite;

        return SolutionType.None;
    }

    public static Matrix zeros(int n_rows, int n_cols)
    {
        return new Matrix(n_rows, n_cols);
    }

    /**
     * Создаёт квадратную матрицу нулей.
     * @param size сторона матрицы.
     * @return матрица из нулей.
     */
    public static Matrix zeros(int size)
    {
        return zeros(size, size);
    }

    /**
     * Создаёт единичную матрицу.
     * @param n_rows число строк.
     * @param n_cols число столбцов.
     * @return единичная матрица.
     */
    public static Matrix identity(int n_rows, int n_cols)
    {
        Matrix I = new Matrix(n_rows, n_cols);
        for (int i = 0; i < Math.min(n_rows, n_cols); i++) I.set(i, i,1.0);
        return I;
    }

    /**
     * LU разложение матрицы на нижнюю и верхнюю треугольные матрицы
     * low - Нижняя треугольная матрица
     * up - Верхняя треугольная матрица
     * @param src Матрица разложение которой нужно провести
     * @return массив из двух матриц, как рещультат LU разложения.
     */
    public static Matrix[] lu( Matrix src)
    {
        Matrix low,  up;

        if (src.cols() != src.rows()) throw new RuntimeException("LU decomposition error::non square matrix");

        low = new Matrix(src.cols(), src.cols());

        up = new Matrix(src.cols(), src.cols());

        int i, j, k;

        for (i = 0; i < src.cols(); i++)
        {
            for (j = 0; j < src.cols(); j++)
            {
                if (j >= i)
                {
                    low.set(j,i,src.get(j,i));

                    for (k = 0; k < i; k++)  low.set(j,i,low.get(j,i) - low.get(j,k) * up.get(k,i));
                }
            }

            for (j = 0; j < src.cols(); j++)
            {
                if (j < i) continue;

                if (j == i)
                {
                    up.set(i,j,1.0);
                    continue;
                }

                up.set(i,j,src.get(i,j) / low.get(i,i));

                for (k = 0; k < i; k++)  up.set(i,j,up.get(i,j) - low.get(i,k) * up.get(k,j) / low.get(i,i));
            }
        }
        return  new Matrix[]{low,up};
    }

    /**
     * Вспомогательный метод рещения системы уравнений вида Ax = b при условии, что найдено разложение A = LU.
     * @param low нижняя треугольная матрица.
     * @param up верхняя треугольняая матрица.
     * @param b вектор свободных членов.
     * @return x = A^-1 * b = (L * U)^-1 * b.
     */
    private static DoubleVector linsolve( Matrix low,  Matrix up,  DoubleVector b)
    {
        double det = 1.0;

        DoubleVector x, z;

        for (int i = 0; i < up.rows(); i++) det *= (up.get(i,i) * up.get(i,i));

        if (Math.abs(det) < 1e-12)
        {
            if(showMatrixDebugLog)  System.out.println("Matrix is probably singular :: unable to solve A^-1 b = x");
            return null;
        }

        z = new DoubleVector(up.rows());

        double tmp;

        for (int i = 0; i < z.size(); i++)
        {
            tmp = 0.0;
            for (int j = 0; j < i; j++) tmp += z.get(j) * low.get(i,j);
            z.set(i, (b.get(i) - tmp )/ low.get(i,i));
        }

        x = new DoubleVector(up.rows());

        for (int i = z.size() - 1; i >= 0; i--)
        {
            tmp = 0.0;
            for (int j = i + 1; j < z.size(); j++) tmp += x.get(j) * up.get(i,j);
            x.set(i, z.get(i) - tmp);
        }
        return x;
    }

    /**
     * Решение системы уравнений вида Ax = b.
     * @param mat матрица СЛАУ.
     * @param b вектор свободных членов.
     * @return x = A^-1 * b.
     */
    public static DoubleVector linsolve(Matrix mat, DoubleVector b)
    {
        if (mat.rows() != mat.cols()) throw new RuntimeException("non square matrix");
        Matrix[]lu_ = lu(mat);
        return linsolve( lu_[0],  lu_[1], b);
    }

    /**
     * Рассчитывает обратную матрицу A^-1.
     * @param mat исходная квадратная матрица.
     * @return A^-1.
     */
    public static Matrix invert(Matrix mat)
    {
        if (mat.rows() != mat.cols()) throw new RuntimeException("non square matrix");

        Matrix[]lu_ = lu(mat);

        double det = 1.0;

        for (int i = 0; i < lu_[0].rows(); i++) det *= (lu_[0].get(i,i) * lu_[0].get(i,i));

        if (Math.abs(det) < 1e-12)
        {
            if(showMatrixDebugLog) System.out.println("Matrix is probably singular :: unable to calculate invert matrix");
            return null;
        }

        DoubleVector b, col;

        b = new DoubleVector(mat.rows());

        Matrix inv = zeros(mat.rows());

        for (int i = 0; i < mat.cols(); i++)
        {
            b.set(i, 1.0);
            col = linsolve( lu_[0], lu_[1], b);

            if (col == null) throw new RuntimeException("unable to find matrix inversion");

            if (col.size() == 0) throw new RuntimeException("unable to find matrix inversion");

            b.set(i, 0.0);

            for (int j = 0; j < mat.rows(); j++) inv.set(j,i,col.get(j));
        }
        return inv;
    }

    /**
     * Транспонирование матрицы
     * @param mat исходная матрица.
     * @return A^T.
     */
    public static Matrix transpose(Matrix mat)
    {
        Matrix trans = new Matrix(mat.cols(), mat.rows());
        for (int i = 0; i < mat.rows(); i++) for (int j = 0; j < mat.cols(); j++)  trans.set(j, i, mat.get(i,j));
        return trans;
    }

    ///////////////////////////////
    //      ADDITION INTERNAL    //
    ///////////////////////////////
    public Matrix add(Matrix other)
    {
        if(rows()!= other.rows())  throw new RuntimeException("Dot product :: this.Size()!= other.Size()");
        if(cols()!= other.rows()) throw new RuntimeException("Dot product :: this.Size()!= other.Size()");
        for (int i = 0; i < rows(); i++) row(i).add(other.row(i));
        return  this;
    }

    public Matrix add(double other)
    {
        for (int i = 0; i < rows(); i++) row(i).add(other);
        return  this;
    }

    ///////////////////////////////
    //    SUBTRACTION INTERNAL   //
    ///////////////////////////////
    public Matrix sub(Matrix other)
    {
        if(rows()!= other.rows()) throw new RuntimeException("Dot product :: this.Size()!= other.Size()");
        if(cols()!= other.rows()) throw new RuntimeException("Dot product :: this.Size()!= other.Size()");
        for (int i = 0; i < rows(); i++)  row(i).sub(other.row(i));
        return  this;
    }

    public Matrix sub(double other)
    {
        for (int i = 0; i < rows(); i++) row(i).sub(other);
        return  this;
    }

    ///////////////////////////////
    //  MULTIPLICATION INTERNAL  //
    ///////////////////////////////
    public Matrix mul(double other)
    {
        for (int i = 0; i < rows(); i++)  row(i).mul(other);
        return  this;
    }
    // public Matrix mul(Matrix other) ...

    ///////////////////////////////
    //     DIVISION INTERNAL     //
    ///////////////////////////////
    public Matrix div(double other)
    {
        return  this.mul(1.0 / other);
    }

    // public Matrix div(Matrix other) ...

    ///////////////////////////////////
    ////  MULTIPLICATION EXTERNAL  ////
    ///////////////////////////////////
    public static Matrix mul(Matrix a, Matrix b)
    {
        if (a.cols() != b.rows())  throw new RuntimeException("Error matrix multiplication::a.NCols != b.NRows");
        Matrix b_t = transpose(b);
        Matrix res = new Matrix(a.rows(), b.cols());
        for (int i = 0; i < a.rows(); i++)for (int j = 0; j < b.cols(); j++) res.set(i, j, DoubleVector.dot(a.row(i), b_t.row(j)));
        return res;
    }

    public static DoubleVector mul(Matrix mat, DoubleVector vec)
    {
        if (mat.cols() != vec.size())  throw new RuntimeException("unable to matrix and vector multiply");

        DoubleVector result = new DoubleVector(mat.rows());
        int cntr = 0;
        for (DoubleVector row : mat)
        {
            result.set(cntr++, DoubleVector.dot(row, vec));
        }
        return result;
    }

    public static DoubleVector mul(DoubleVector vec, Matrix mat)
    {
        if (mat.rows() != vec.size())  throw new RuntimeException("unable to matrix and vector multiply");

        DoubleVector result = new DoubleVector(mat.cols());

        for (int i = 0; i < mat.cols(); i++)
        {
            for (int j = 0; j < mat.rows(); j++) result.set(i, mat.get(j,i) * vec.get(i));
        }

        return result;
    }

    public static Matrix mul(Matrix mat, double a)
    {
        Matrix result = new Matrix(mat);
        return result.mul(a);
    }

    public static Matrix mul(double a, Matrix mat)
    {
        return mul(mat , a);
    }

    ///////////////////////////////////
    ////     ADDITION EXTERNAL     ////
    ///////////////////////////////////
    public static Matrix add(Matrix a, Matrix b)
    {
        if (a.cols() != b.cols()) throw new RuntimeException("unable to add matrix a to matrix b");

        if (a.rows() != b.rows()) throw new RuntimeException("unable to add matrix a to matrix b");

        Matrix result = new Matrix(a);

        return result.add(b);
    }

    public static Matrix add(Matrix a, double b)
    {
        Matrix result = new Matrix(a);

        return result.add(b);
    }

    public static Matrix add(double b, Matrix a)
    {
        return add(a , b);
    }

    ///////////////////////////////////
    ////    SUBTRACTION EXTERNAL   ////
    ///////////////////////////////////
    public static Matrix sub(Matrix a, Matrix b)
    {
        if (a.cols() != b.cols()) throw new RuntimeException("unable to add matrix a to matrix b");

        if (a.rows() != b.rows()) throw new RuntimeException("unable to add matrix a to matrix b");

        Matrix result = new Matrix(a);

        return result.sub(b);
    }

    public static Matrix sub(Matrix a, double b)
    {
        Matrix result = new Matrix(a);

        return result.sub(b);
    }

    public static Matrix sub(double b, Matrix a)
    {
        Matrix result = new Matrix(a);

        for (int i = 0; i < a.rows(); i++)
        {
            result.set(i, DoubleVector.sub(b, a.row(i)));
        }
        return result;
    }
}