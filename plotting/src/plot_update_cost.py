import os

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

import constants


def plot_cache(input_file):
    plt.figure(figsize=(12.7, 6.27))
    df = pd.read_csv(input_file, delimiter=',')
    df['TIME'] = df['TIME'].apply(lambda x: x / 1e+9)[df['TIME'] > 0]
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "Hot Update" if "UPDATE" in x else x) \
        .map(lambda x: "Cold Update" if "DISCUPT" in x else x) \
        .map(lambda x: "Full Build" if "BUILD" in x else x)
    df = df[~df['TYPE'].str.contains("Graph")]
    df = df[~df['TYPE'].str.contains("Expired Cache")]
    df = df.rename(columns={
        'FILE_NAME': 'Library',
        'PHASE': 'Update Type',
        'TIME': 'Time [Seconds]',
        'TYPE': 'Update Measure'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x='Time [Seconds]', hue='Update Measure',
                orient="h",
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()
    plt.savefig("../pdf/update_cache_cost.pdf")
    plt.savefig("../png/update_cache_cost.png")
    plt.show()


def plot_graph(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['TIME'] = df['TIME'].apply(lambda x: x / 1e+9)[df['TIME'] > 0]
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "Hot Update" if "UPDATE" in x else x) \
        .map(lambda x: "Cold Update" if "DISCUPT" in x else x) \
        .map(lambda x: "Full Build" if "BUILD" in x else x)
    df = df[df['TYPE'].str.contains("Graph")]
    df = df.rename(columns={
        'FILE_NAME': 'Library',
        'PHASE': 'Update Type',
        'TIME': 'Time [Seconds]',
        'TYPE': 'Update Measure'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x='Time [Seconds]', hue='Update Measure',
                orient="h",
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()
    plt.savefig("../pdf/update_graph.pdf")
    plt.savefig("../png/update_graph.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot_cache(constants.UPDATE_FILE)
    plot_graph(constants.UPDATE_FILE)
