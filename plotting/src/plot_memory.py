import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

import constants


def plot(input_file):
    plt.figure(figsize=(8.27, 10.7))
    df = pd.read_csv(input_file, delimiter=',')
    df['Memory'] = df['Memory'].apply(lambda x: x / 1024e6)
    df = df.rename(columns={'Memory': 'Memory [Gb]'})
    sns.set(style="darkgrid")
    # Create the boxplot
    sns.boxenplot(y="Project", x="Memory [Gb]", data=df, orient="h",)
    sns.stripplot(y="Project", x="Memory [Gb]", data=df, orient="h", size=2, color=".26")
    # sns.boxenplot(x="Project", y="Memory [Gb]", data=df)
    # plt.title("Process Memory Consumption")
    plt.xticks(rotation=15)
    plt.tight_layout()
    plt.savefig("memory.pdf")
    plt.show()


if __name__ == '__main__':
    plot(constants.MEMORY_FILE)
