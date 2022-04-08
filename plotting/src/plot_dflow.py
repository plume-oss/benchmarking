import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['TIME'] = df['TIME'].apply(lambda row: row / 1e+9)
    df = df.drop(df[df['PHASE'].str.contains("Initial")].index)
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "No Sharing" if "Do not share cache with local search" in x else x) \
        .map(lambda x: "Recycle & Share" if "Recycle and share cache with" in x else x) \
        .map(lambda x: "Recycle & No Sharing" if "Recycle with no sharing" in x else x) \
        .map(lambda x: "Share" if "Share cache on local search" in x else x)

    df = df.rename(columns={
        'TIME': 'Time [Seconds]',
        'FILE_NAME': 'Library',
        'DATABASE': 'Database',
        'PHASE': 'Cache Use Strategy'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="Time [Seconds]",
                hue="Cache Use Strategy",
                orient="h",
                aspect=8 / 11,
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()

    plt.savefig("../pdf/taint.pdf")
    plt.savefig("../png/taint.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.TAINT_PERFORMANCE_FILE)
