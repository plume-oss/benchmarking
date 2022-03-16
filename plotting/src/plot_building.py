import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import constants


def plot(input_file):
    df = pd.read_csv(input_file, delimiter=',')
    df['TIME_SEC'] = df['TIME'].apply(lambda x: x / 1e+9)
    df['PHASE'] = df['PHASE'] \
        .map(lambda x: "Online Update" if "UPDATE" in x else x) \
        .map(lambda x: "Disconnected Update" if "DISCUPT" in x else x) \
        .map(lambda x: "Full Build" if "INIT" in x or "BUILD" in x else x)

    df = df.rename(columns={
        'TIME_SEC': 'Time [Seconds]',
        'FILE_NAME': 'Library',
        'DATABASE': 'Database',
        'PHASE': 'Update Type'
    })

    sns.catplot(data=df, kind="bar",
                y="Library", x="Time [Seconds]", hue="Update Type",
                orient="h",
                alpha=.6, height=6,
                aspect=8.27 / 10.7,
                order=["RxJava", "guava", "fastjson", "spring-boot", "jackson-core", "mybatis-3", "mockito",
                       "guice", "scribejava"]
                )
    plt.xticks(rotation=10)
    plt.tight_layout()
    plt.savefig("build.pdf")
    plt.show()


if __name__ == '__main__':
    plot(constants.RESULT_FILE)
