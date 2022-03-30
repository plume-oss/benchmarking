import os

import pandas as pd
import seaborn as sns
from matplotlib import pyplot as plt

import constants


def plot(input_file):
    plt.figure(figsize=(10.7, 6.27))
    df = pd.read_csv(input_file, delimiter=',')
    df.info()
    df['TIME_SEC'] = df['TIME'].apply(lambda x: x / 1e+9)

    update_df = df[df['PHASE'].str.contains('UPDATE') | df['PHASE'].str.contains('DISCUP')]
    build_df = df[df['PHASE'].str.contains('BUILD')]
    update_df['PHASE'] = update_df['PHASE'] \
        .map(lambda x: "Hot Update" if "UPDATE" in x else x) \
        .map(lambda x: "Cold Update" if "DISCUPT" in x else x)
    update_df = update_df.rename(columns={
        'TIME_SEC': 'Time [Seconds]',
        'FILE_NAME': 'Library',
        'PROGRAM_CLASSES': 'No. Changed Classes Per Commit',
        'PROGRAM_METHODS': 'No. Affected Methods Per Commit',
        'PHASE': 'Update Type'
    })

    libs = df.drop_duplicates(subset=["FILE_NAME"])["FILE_NAME"].values.flatten().tolist()
    colors = sns.color_palette(n_colors=len(libs)).as_hex()

    lib_to_col = {}
    i = 0
    for col, lib in zip(colors, libs):
        lib_to_col[lib] = col
        i += 1

    x_axis = "No. Affected Methods Per Commit"
    # x_axis = "No. Changed Classes Per Commit"
    ax = sns.scatterplot(data=update_df, x=x_axis, y="Time [Seconds]",
                         hue="Library", alpha=.6, palette=lib_to_col,
                         style=update_df['Update Type'])

    for lib in libs:
        avg_build = build_df[build_df['FILE_NAME'].str.contains(lib)]['TIME_SEC'].mean()
        plt.axhline(y=avg_build, c=lib_to_col[lib], linestyle='dashed', zorder=-1)

    if x_axis == "No. Affected Methods Per Commit":
        ax.set(xlim=(-5, 300))
    else:
        ax.set(xlim=(-1, 105))
    ax.set(ylim=(-2, 38))
    plt.tight_layout()

    plt.savefig("../pdf/change_relation.pdf")
    plt.savefig("../png/change_relation.png")
    plt.show()


if __name__ == '__main__':
    if not os.path.exists("../pdf"):
        os.makedirs("../pdf")
    if not os.path.exists("../png"):
        os.makedirs("../png")
    plot(constants.RESULT_FILE)
