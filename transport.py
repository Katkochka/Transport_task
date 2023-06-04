import numpy as np
import xlsxwriter


def check_open(reserves, needs):
    sumr = 0
    sumn = 0
    for i in needs0:
        sumn += i
    for i in reserves:
        sumr += i
    if (sumn == sumr[0]):
        print('Закрытый')
        print(sumn, '=', sumr[0], '\n')
    else:
        print('Открытый')
        print(sumn, '/=', sumr[0], '\n')
        return (1)


def simplex_table_open(needs, reserves):
    I = np.eye(needs.size)
    one_vec = np.ones(needs.size)
    t = I
    for i in range(reserves.size - 1):
        t = np.concatenate((t, I), axis=1)  # строим единичные матрицы
    up = 0
    for i in reversed(range(reserves.size)):
        vec = np.concatenate(([0 for _ in range((needs.size) * (reserves.size - i - 1))], one_vec), axis=None)
        t_1 = np.concatenate((vec, [0 for _ in range((needs.size) * i)]), axis=None)
        if (i == reserves.size - 1):
            up = t_1
        else:
            up = np.vstack([up, t_1])
    t = np.vstack([up,t])

    if check_open(reserves, needs):
        x = [[0] * reserves.size for _ in range(needs.size)]
        I = np.eye(reserves.size)
        t_2 = np.concatenate((I, x), axis=0)
        t = np.concatenate((t, t_2), axis=1)
        print(t)

    for row_i, row in enumerate(t):
        print('|'.join(f"{c1:^6}" for c1 in row), end="")
        print(f"|", end="\n")
    print()
    return t


# reserves = np.array([[10], [5]])
# needs = np.array([[6, 9]])
# count = np.array([[1, 2,],
#                   [3, 1]])

# reserves = np.array([[10], [4], [1]])
# needs = np.array([[6, 9]])
# count = np.array([[1, 2, ],
#                   [3, 1],
#                   [2, 2]])

# reserves = np.array([[140], [140], [120]])
# needs = np.array([[115, 65, 90, 130]])
# count = np.array([[1, 8, 2, 9],
#                   [8, 7, 5, 1],
#                   [5, 3, 2, 4]])

reserves = np.array([[110], [150], [120]])
needs = np.array([[115, 65, 90, 130]])
count = np.array([[1, 8, 2, 9],
                  [8, 7, 5, 1],
                  [5, 3, 2, 4]])

t = np.concatenate((count, reserves), axis=1)
needs0 = np.append(needs, [0])
t = np.insert(t, [reserves.size], needs0, axis=0)

for row_i, row in enumerate(t):
    print('|'.join(f"{c1:^9}" for c1 in row), end="")
    print(f"|", end="\n")
print()

trans_table = simplex_table_open(needs, reserves)

workbook = xlsxwriter.Workbook('transport_table.xlsx')
worksheet = workbook.add_worksheet()

row = 0
col = 0

for i in range(trans_table.shape[0]):  # запись в эксель условий
    for j in range(trans_table.shape[1]):
        worksheet.write(i, j, trans_table[i][j])
worksheet.write(trans_table.shape[0], 0, '*')

for i in range(reserves.size):  # запись b
    worksheet.write(trans_table.shape[0] + 1, i, reserves[i])
worksheet.write(trans_table.shape[0] + 2, 0, '*')

for i in range(needs.size):  # запись b
    worksheet.write(trans_table.shape[0] + 3, i, needs[0][i])
worksheet.write(trans_table.shape[0] + 4, 0, '*')

j_str = 0
print(count.shape)
for i in range(count.shape[0]):  # запись с
    for j in range(count.shape[1]):
        worksheet.write(trans_table.shape[0] + 5, j_str, count[i][j])
        j_str += 1

workbook.close()
