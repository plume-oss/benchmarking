import pandas as pd

import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df = df[df['PHASE'].str.contains("UPDATE")]
    cs = df.groupby(['FILE_NAME'])['CHANGED_CLASSES'].mean().astype(int).reset_index()
    ms = df.groupby(['FILE_NAME'])['CHANGED_METHODS'].mean().astype(int).reset_index()
    ms['CHANGED_CLASSES'] = cs['CHANGED_CLASSES']
    ms["METHOD_PER_CLASS"] = (ms['CHANGED_METHODS'] / cs['CHANGED_CLASSES']).astype(int)
    print(ms.head(9))


if __name__ == '__main__':
    plot(constants.RESULT_FILE)
