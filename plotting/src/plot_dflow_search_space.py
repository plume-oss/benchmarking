import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['PATH_TYPE'] = df['PATH_TYPE'].map(lambda x: "Reaching Flows" if "Flows" in x else x)
    df = df.rename(columns={
        'FILE_NAME': 'Library',
        'PATH_TYPE': 'Query Result Type',
        'PATH_COUNT': 'Query Result Count'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x='Query Result Count', hue='Query Result Type',
                orient="h",
                alpha=.6, height=6,
                aspect=8.27 / 10.7,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()

    plt.savefig("../pdf/dflow_search.pdf")
    plt.savefig("../png/dflow_search.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.TAINT_SEARCH_FILE)
