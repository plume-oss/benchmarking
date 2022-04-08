import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def cache_hit_ratio(row):
    return float(row["CACHE_HITS"]) / (float(row["CACHE_HITS"]) + float(row["CACHE_MISSES"]) + 0.000000001) * 100.0


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df = df.drop(df[df['PHASE'].str.contains("Initial")].index)
    df['CACHE_RATIO'] = df.apply(lambda x: cache_hit_ratio(x), axis=1)
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "No Sharing" if "Do not share cache with local search" in x else x) \
        .map(lambda x: "Recycle & Share" if "Recycle and share cache with" in x else x) \
        .map(lambda x: "Recycle & No Sharing" if "Recycle with no sharing" in x else x) \
        .map(lambda x: "Share" if "Share cache on local search" in x else x)

    df = df.rename(columns={
        'CACHE_RATIO': 'Cache Hit Ratio (%)',
        'FILE_NAME': 'Library',
        'DATABASE': 'Database',
        'PHASE': 'Cache Strategy'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="Cache Hit Ratio (%)", hue="Cache Strategy",
                orient="h",
                alpha=.6, height=6,
                aspect=8 / 10,
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
