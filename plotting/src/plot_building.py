import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import constants


def plot_phase(df, title, cond):
    sns.catplot(data=df[cond], kind="bar",
                x="Project", y="Time [Minutes]", hue="Database",
                alpha=.6, height=6)
    plt.title(title)
    plt.xticks(rotation=45)
    plt.show()


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df.info()
    df['TIME'] = df[constants.TIME_COLUMNS].sum(axis=1).apply(lambda x: x / 6e+10)
    df.info()
    df = df.rename(columns={
        'TIME': 'Time [Minutes]',
        'FILE_NAME': 'Project',
        'DATABASE': 'Database'
    })
    plot_phase(df, 'Full Builds', (df['PHASE'].str.contains('INIT')) | (df['PHASE'].str.contains('BUILD')))
    plot_phase(df, 'Online Updates', df['PHASE'].str.contains('UPDATE'))
    plot_phase(df, 'Disconnected Updates', df['PHASE'].str.contains('DISCUPT'))
