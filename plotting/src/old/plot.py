import math

import matplotlib.pyplot as plt
import numpy as np
from typing import List

from plot_benchmarks import Benchmark





def plot_cpg_performance(v: str, results: dict):
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
            ax.set_title("{}".format(f_name))
            ax.set_ylabel('Wall Clock Time Elapsed (min)')
            ax.set_xlabel('Database')
            lac, ugb, pstr, cpg, scpgp = [], [], [], [], []
            for db in x_axis_dbs:
                # Build each column
                lac.append(np.mean([int(x["COMPILING_AND_UNPACKING"]) * 10 ** -11 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                ugb.append(np.mean([int(x["SOOT"]) * 10 ** -11 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                pstr.append(np.mean([int(x["PROGRAM_STRUCTURE_BUILDING"]) * 10 ** -11 for x in results if
                                     x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                cpg.append(np.mean([int(x["BASE_CPG_BUILDING"]) * 10 ** -11 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                scpgp.append(np.mean([int(x["DATA_FLOW_PASS"]) * 10 ** -11 for x in results if
                                      x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
            ax.bar(x_axis_dbs, lac, 0.35, label='Loading and Compiling')
            ax.bar(x_axis_dbs, ugb, 0.35, label='Soot Related Processing')
            ax.bar(x_axis_dbs, cpg, 0.35, label='Program Structure Building')
            ax.bar(x_axis_dbs, cpg, 0.35, label='Base CPG Building')
            ax.bar(x_axis_dbs, scpgp, 0.35, label='Running Data Flow Passes')
            fi += 1
            ax.label_outer()
    handles, labels = plt.gca().get_legend_handles_labels()
    by_label = dict(zip(labels, handles))
    plt.legend(by_label.values(), by_label.keys(), loc='lower center', bbox_to_anchor=[-0.1, -0.7])
    fig.subplots_adjust(bottom=0.25)
    fig.suptitle("CPG Performance | Plume Version {}".format(v), fontsize=20)
    fig.savefig("./results/PlumeV{}_CPG_Performance.pdf".format(v))


def plot_db_performance(v: str, results: dict):
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
            file_size = get_file_size(f_name)
            ax.set_title("{} ({:.3f}Kb)".format(f_name, file_size))
            ax.set_ylabel('CPU Clock Time Elapsed (min)')
            ax.set_xlabel('Database')
            dbw, dbr = [], []
            for db in x_axis_dbs:
                # Build each column
                dbw.append(np.mean([int(x["DATABASE_WRITE"]) * 10 ** -11 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
                dbr.append(np.mean([int(x["DATABASE_READ"]) * 10 ** -11 for x in results if
                                    x["DATABASE"] == db and x["FILE_NAME"] == f_name]))
            ax.bar(x_axis_dbs, dbw, 0.35, label='Database Writes')
            ax.bar(x_axis_dbs, dbr, 0.35, label='Database Reads')
            fi += 1
            ax.label_outer()
    handles, labels = plt.gca().get_legend_handles_labels()
    by_label = dict(zip(labels, handles))
    plt.legend(by_label.values(), by_label.keys(), loc='lower center', bbox_to_anchor=[-0.1, -0.7])
    fig.subplots_adjust(bottom=0.25)
    fig.suptitle("Database Performance | Plume Version {}".format(v), fontsize=20)
    fig.savefig("./results/PlumeV{}_DB_Performance.pdf".format(v))
