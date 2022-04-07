import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def plot(cpg_input, taint_input):
    df_cpg = pd.read_csv(cpg_input, delimiter=',')
    df_taint = pd.read_csv(taint_input, delimiter=',')

    df_cpg['Memory'] = df_cpg['Memory'].apply(lambda x: x / 1000e6)[df_cpg['Memory'] > 0]
    df_cpg = df_cpg.rename(columns={
        'Memory': 'Memory [GB]',
        'Project': 'Library'
    })
    df_taint['Memory'] = df_taint['Memory'].apply(lambda x: x / 1000e6)[df_taint['Memory'] > 0]
    df_taint = df_taint.rename(columns={
        'Memory': 'Memory [GB]',
        'Project': 'Library'
    })
    df_all = pd.concat([df_cpg, df_taint], ignore_index=True, sort=False)

    # libs = df_cpg.drop_duplicates(subset=["Project"])["Project"].values.flatten().tolist()
    # dbDf = df_cpg[~df_cpg['Database'].str.contains("Soot")]
    # sootDf = df_cpg[df_cpg['Database'].str.contains("Soot")]

    sns.set(style="darkgrid")
    # Create the boxplot
    sns.boxenplot(y="Library", x="Memory [GB]", data=df_all, orient="h",
                  order=constants.PLOT_ORDER)
    # sns.swarmplot(y="Library", x="Memory [Gb]", data=df_taint, orient="h", size=2, color="red",
    #               order=constants.PLOT_ORDER)
    # sns.swarmplot(y="Library", x="Memory [Gb]", data=df_cpg, orient="h", size=2, color="magenta",
    #               order=constants.PLOT_ORDER)

    # ymin, ymax = (0, 1)
    # yrange = ymin + math.fabs(ymax)
    # buffer = 0.05
    # yset = yrange / len(libs)
    # for i, lib in enumerate(libs):
    #     curr_y = yset * i + 0.055
    #     avg_build = sootDf[sootDf['Project'].str.contains(lib)]['Memory [Gb]'].mean()
    #     plt.axvline(x=avg_build, ymin=curr_y - buffer, ymax=curr_y+buffer, c="red", linestyle='dashed', zorder=5)

    plt.tight_layout()
    plt.savefig("../pdf/memory.pdf")
    plt.savefig("../png/memory.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.CPG_MEMORY_FILE, constants.TAINT_MEMORY_FILE)
