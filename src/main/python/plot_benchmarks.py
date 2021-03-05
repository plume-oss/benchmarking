import csv
import math

import matplotlib.pyplot as plt
import numpy as np


def plot_results(v: str, results: dict):
    # x-axis are the databases
    x_axis_dbs = list(set([x["DATABASE"] for x in results]))
    # each subplot is the file name
    plots_files = list(set([x["FILE_NAME"] for x in results]))
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
            lac, ugb, dbw, dbr, scpgp = [], [], [], [], []
            for db in x_axis_dbs:
                # Build each column
                lac.append(np.mean([int(x["LOADING_AND_COMPILING"]) * 10 ** -9 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                ugb.append(np.mean([int(x["UNIT_GRAPH_BUILDING"]) * 10 ** -9 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                dbw.append(np.mean([int(x["DATABASE_WRITE"]) * 10 ** -9 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                dbr.append(np.mean([int(x["DATABASE_READ"]) * 10 ** -9 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                scpgp.append(np.mean([int(x["SCPG_PASSES"]) * 10 ** -9 for x in results if
                                      x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
            ax.bar(x_axis_dbs, lac, 0.35, label='Loading and Compiling')
            ax.bar(x_axis_dbs, ugb, 0.35, label='Building Unit Graphs with Soot')
            ax.bar(x_axis_dbs, dbw, 0.35, label='Database Writes')
            ax.bar(x_axis_dbs, dbr, 0.35, label='Database Reads')
            ax.bar(x_axis_dbs, scpgp, 0.35, label='Running SCPG Passes')
            fi += 1
            ax.label_outer()
    handles, labels = plt.gca().get_legend_handles_labels()
    by_label = dict(zip(labels, handles))
    plt.legend(by_label.values(), by_label.keys(), loc='lower center', bbox_to_anchor=[-0.1, -0.7])
    fig.subplots_adjust(bottom=0.25)
    fig.suptitle("Plume Version {}".format(v), fontsize=20)
    fig.savefig("ResultPlumeV{}.pdf".format(v))


with open('results.csv') as csv_file:
    csv_reader = csv.DictReader(csv_file, delimiter=',')
    plots_per_version = {}
    for row in csv_reader:
        plumeV = str(row["PLUME_VERSION"])
        if plumeV not in plots_per_version:
            plots_per_version[plumeV] = []
        plots_per_version[plumeV].append({
            'FILE_NAME': str(row["FILE_NAME"]),
            'DATABASE': str(row["DATABASE"]),
            'LOADING_AND_COMPILING': int(row["LOADING_AND_COMPILING"]),
            'UNIT_GRAPH_BUILDING': int(row["UNIT_GRAPH_BUILDING"]),
            'DATABASE_WRITE': int(row["DATABASE_WRITE"]),
            'DATABASE_READ': int(row["DATABASE_READ"]),
            'SCPG_PASSES': int(row["SCPG_PASSES"])
        })

    for (ver, res) in plots_per_version.items():
        plot_results(ver, res)
        plt.clf()
