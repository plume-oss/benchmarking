import pandas as pd

import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df = df[df['PHASE'].str.contains("UPDATE")]
    print(df.groupby(['FILE_NAME'])['CHANGED_CLASSES'].mean().astype(int).reset_index())
    print(df.groupby(['FILE_NAME'])['CHANGED_METHODS'].mean().astype(int).reset_index())


if __name__ == '__main__':
    plot(constants.RESULT_FILE)
