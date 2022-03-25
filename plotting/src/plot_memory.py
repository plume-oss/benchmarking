import math
import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def plot(input_file):
    plt.figure(figsize=(8.27, 10.7))
    df = pd.read_csv(input_file, delimiter=',')
    df['Memory'] = df['Memory'].apply(lambda x: x / 1024e6)[df['Memory'] > 0]
    df = df.rename(columns={
        'Memory': 'Memory [Gb]',
        'Project': 'Library'
    })

    libs = df.drop_duplicates(subset=["Project"])["Project"].values.flatten().tolist()
    dbDf = df[~df['Database'].str.contains("Soot")]
    sootDf = df[df['Database'].str.contains("Soot")]

    sns.set(style="darkgrid")
    # Create the boxplot
    sns.boxenplot(y="Project", x="Memory [Gb]", data=dbDf, orient="h",
                  order=constants.PLOT_ORDER)
    # sns.stripplot(y="Project", x="Memory [Gb]", data=dbDf, orient="h", size=2, color=".26",
    #               order=constants.PLOT_ORDER)

    ymin, ymax = (0, 1)
    yrange = ymin + math.fabs(ymax)
    buffer = 0.05
    yset = yrange / len(libs)
    for i, lib in enumerate(libs):
        curr_y = yset * i + 0.055
        avg_build = sootDf[sootDf['Project'].str.contains(lib)]['Memory [Gb]'].mean()
        plt.axvline(x=avg_build, ymin=curr_y - buffer, ymax=curr_y+buffer, c="red", linestyle='dashed', zorder=5)

    plt.tight_layout()
    plt.savefig("../pdf/memory.pdf")
    plt.savefig("../png/memory.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.MEMORY_FILE)
