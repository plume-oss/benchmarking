import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df.info()
    df['Memory'] = df['Memory'].apply(lambda x: x / 1024e3)
    df = df.rename(columns={'Memory': 'Memory [Mb]'})
    sns.set(style="darkgrid")
    # Create the boxplot
    sns.violinplot(x="Project", y="Memory [Mb]", hue="Database", cut=0, data=df)
    plt.show()
