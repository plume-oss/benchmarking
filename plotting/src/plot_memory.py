import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['Memory'] = df['Memory'].apply(lambda x: x / 1024e6)
    df = df.rename(columns={'Memory': 'Memory [Gb]'})
    sns.set(style="darkgrid")
    # Create the boxplot
    sns.violinplot(x="Project", y="Memory [Gb]", cut=0, data=df, aspect=12.7 / 8.27)
    # plt.title("Process Memory Consumption")
    plt.xticks(rotation=15)
    plt.tight_layout()
    plt.savefig("memory.pdf")
