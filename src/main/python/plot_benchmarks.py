import csv
import math

import matplotlib.pyplot as plt


def plot_results(v: str, results: dict):
    # x-axis are the databases
    x_axis_dbs = list(set([x["database"] for x in results]))
    # each subplot is the file name
    plots_files = list(set([x["fileName"] for x in results]))
    xg, yg = math.floor(len(plots_files) / 2), math.ceil(len(plots_files) / 2)
    fig, ax_tup = plt.subplots(xg, yg, figsize=(8, 8))

    fi = 0
    for x in range(xg):
        for y in range(yg):
            ax = ax_tup[x, y]
            f_name = plots_files[fi]
            ax.set_title(f_name)
            ax.set_ylabel('Time Elapsed (min)')
            ax.set_xlabel('Database')
            for db in x_axis_dbs:
                # Build each column
                lac = [int(x["loadingAndCompiling"]) * 10 ** -11 for x in results if
                       x["database"] == db and x["fileName"] == f_name]
                bs = [int(x["buildSoot"]) * 10 ** -11 for x in results if
                      x["database"] == db and x["fileName"] == f_name]
                bp = [int(x["buildPasses"]) * 10 ** -11 for x in results if
                      x["database"] == db and x["fileName"] == f_name]
                ax.bar(x_axis_dbs, lac, 0.35, label='Loading and Compiling')
                ax.bar(x_axis_dbs, bs, 0.35, label='Building CPG with Soot')
                ax.bar(x_axis_dbs, bp, 0.35, label='Running Internal Passes')
            fi += 1
            ax.label_outer()
    handles, labels = plt.gca().get_legend_handles_labels()
    by_label = dict(zip(labels, handles))
    plt.legend(by_label.values(), by_label.keys(), loc='lower center', bbox_to_anchor=[-0.1, -0.5])
    fig.subplots_adjust(bottom=0.2)
    fig.suptitle("Plume Version {}".format(v), fontsize=20)
    fig.savefig("ResultPlumeV{}.pdf".format(v))


with open('results.csv') as csv_file:
    csv_reader = csv.DictReader(csv_file, delimiter=',')
    line_count = 0
    plots_per_version = {}
    for row in csv_reader:
        plumeV = str(row["plumeVersion"])
        if plumeV not in plots_per_version:
            plots_per_version[plumeV] = []
        plots_per_version[plumeV].append({
            'fileName': str(row["fileName"]),
            'database': str(row["database"]),
            'loadingAndCompiling': int(row["loadingAndCompiling"]),
            'buildSoot': int(row["buildSoot"]),
            'buildPasses': int(row["buildPasses"])
        })

    for (ver, res) in plots_per_version.items():
        plot_results(ver, res)
        plt.clf()

    print(f'Processed {line_count} lines.')
