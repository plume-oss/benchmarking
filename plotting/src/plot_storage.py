import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df.info()
    df['FILE_SIZE_MB'] = df['FILE_SIZE'].apply(lambda x: x / 1000 ** 2)

    df = df.rename(columns={
        'FILE_SIZE_MB': 'File Size [MB]',
        'FILE_TYPE': 'File Type',
        'FILE_NAME': 'Library',
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="File Size [MB]", hue="File Type",
                orient="h",
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )
    plt.tight_layout()

    plt.savefig("../pdf/storage.pdf")
    plt.savefig("../png/storage.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.STORAGE_FILE)
