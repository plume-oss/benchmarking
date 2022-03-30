import os

import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def add_serialization_time(row):
    return (row['TIME'] + row['CONNECT_DESERIALIZE'] + row['DISCONNECT_SERIALIZE']) / 1e+9


def get_serialization_time(row):
    return (row['CONNECT_DESERIALIZE'] + row['DISCONNECT_SERIALIZE']) / 1e+9


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['TIME'] = df.apply(lambda row: add_serialization_time(row), axis=1)
    df = df.drop(df[df['PHASE'].str.contains("INIT")].index)
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "Hot Update" if "UPDATE" in x else x) \
        .map(lambda x: "Cold Update" if "DISCUPT" in x else x) \
        .map(lambda x: "Full Build" if "BUILD" in x else x)

    df = df.rename(columns={
        'TIME': 'Time [Seconds]',
        'TIME_SER': 'Serialization Time [Seconds]',
        'FILE_NAME': 'Library',
        'DATABASE': 'Database',
        'PHASE': 'Update Type'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="Time [Seconds]", hue="Update Type",
                orient="h",
                alpha=.6, height=6,
                order=constants.PLOT_ORDER
                )

    plt.tight_layout()

    plt.savefig("../pdf/build.pdf")
    plt.savefig("../png/build.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.RESULT_FILE)
