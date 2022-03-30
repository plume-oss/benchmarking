import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def cache_hit_ratio(row):
    return float(row["CACHE_HITS"]) / (float(row["CACHE_HITS"]) + float(row["CACHE_MISSES"])) * 100.0


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df = df.drop(df[df['PHASE'].str.contains("INIT")].index)
    df = df.drop(df[df['PHASE'].str.contains("UPDATE")].index)
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "Recycle Cache" if "DISCUPT" in x else x) \
        .map(lambda x: "Discard Cache" if "BUILD" in x else x)
    df['CACHE_RATIO'] = df.apply(lambda x: cache_hit_ratio(x), axis=1)

    df = df.rename(columns={
        'CACHE_RATIO': 'Cache Hit Ratio (%)',
        'FILE_NAME': 'Library',
        'DATABASE': 'Database',
        'PHASE': 'Cache Use Strategy'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="Cache Hit Ratio (%)", hue="Cache Use Strategy",
                orient="h",
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()

    plt.savefig("../pdf/dflow_cache.pdf")
    plt.savefig("../png/dflow_cache.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.TAINT_PERFORMANCE_FILE)
