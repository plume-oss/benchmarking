import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df.info()
    df['Data'] = df['Data'].apply(lambda x: x / 1024e3)
    sns.set(style="darkgrid")
    # Create the boxplot
    sns.violinplot(x="Project", y="Data", hue="Database", cut=0, data=df)
    plt.show()
