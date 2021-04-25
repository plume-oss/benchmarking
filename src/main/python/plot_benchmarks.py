import csv
from typing import List

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.lines import Line2D

lbl_font = {
    'family': 'DejaVu Sans',
    'weight': 'normal',
    'size': 8
}


class Benchmark:
    def __init__(self, plume_version, file_name, phase, database, compiling_and_unpacking, soot, program_structure,
                 base_cpg, db_read, db_write, df_pass, cache_hits, cache_misses, connect_deserialize,
                 disconnect_serialize):
        self.plume_version = plume_version
        self.file_name = file_name
        self.phase = phase
        self.database = database
        self.compiling_and_unpacking = compiling_and_unpacking
        self.soot = soot
        self.program_structure = program_structure
        self.base_cpg = base_cpg
        self.db_read = db_read
        self.db_write = db_write
        self.df_pass = df_pass
        self.cache_hits = cache_hits
        self.cache_misses = cache_misses
        self.connect_deserialize = connect_deserialize
        self.disconnect_serialize = disconnect_serialize

    def total_time(self):
        return self.compiling_and_unpacking + self.soot + self.program_structure + self.base_cpg + self.df_pass

    def __str__(self):
        return "Benchmark({}, {}, {})".format(self.file_name, self.database, self.total_time())

    def __repr__(self):
        return str(self)


def avg(rs: List[Benchmark], phase: str):
    return np.mean([r.total_time() for r in rs if r.phase == phase])


def stdd(rs: List[Benchmark], phase: str):
    return np.std([r.total_time() for r in rs if r.phase == phase])


def str_to_ns(ns, unit='s'):
    nanos = np.timedelta64(ns, 'ns')
    unit_time = np.timedelta64(1, unit)
    return nanos / unit_time


intervals = (
    ('h', 3.6e+6),
    ('m', 60 * 1000),
    ('s', 1000),
    ('ms', 1),
)


def display_time(ms, granularity=2):
    result = []

    for name, count in intervals:
        value = ms // count
        if value:
            ms -= value * count
            if value == 1:
                name = name.rstrip('s')
            result.append("{:.0f}{}".format(value, name))
    return ''.join(result[:granularity])


def ns_to_s(ns):
    return ns * 1e-9


def update_build_perf(db: str, ax, rs: List[Benchmark]):
    ax.set_title(db)
    init, init_std = avg(rs, "INITIAL"), stdd(rs, "INITIAL")
    b0, db0 = avg(rs, "BUILD0"), stdd(rs, "BUILD0")
    b1, db1 = avg(rs, "BUILD1"), stdd(rs, "BUILD1")
    b2, db2 = avg(rs, "BUILD2"), stdd(rs, "BUILD2")
    b3, db3 = avg(rs, "BUILD3"), stdd(rs, "BUILD3")
    u0, du0 = avg(rs, "UPDATE0"), stdd(rs, "UPDATE0")
    u1, du1 = avg(rs, "UPDATE1"), stdd(rs, "UPDATE1")
    u2, du2 = avg(rs, "UPDATE2"), stdd(rs, "UPDATE2")
    u3, du3 = avg(rs, "UPDATE3"), stdd(rs, "UPDATE3")
    d0, dd0 = avg(rs, "DISCUPT0"), stdd(rs, "DISCUPT0")
    d1, dd1 = avg(rs, "DISCUPT1"), stdd(rs, "DISCUPT1")
    d2, dd2 = avg(rs, "DISCUPT2"), stdd(rs, "DISCUPT2")
    d3, dd3 = avg(rs, "DISCUPT3"), stdd(rs, "DISCUPT3")

    bs = [ns_to_s(t) for t in [b0, b1, b2, b3]]
    us = [ns_to_s(t) for t in [u0, u1, u2, u3]]
    ds = [ns_to_s(t) for t in [d0, d1, d2, d3]]
    ax.errorbar([0], [ns_to_s(t) for t in [init]], [ns_to_s(t) for t in [init_std]], color='g', marker='o',
                linestyle='None', label="Initial Build")
    ax.errorbar([1, 2, 3, 4], bs, [ns_to_s(t) for t in [db0, db1, db2, db3]],
                color='r', marker='x', linestyle='None', label="Full Build")
    ax.errorbar([1, 2, 3, 4], us, [ns_to_s(t) for t in [du0, du1, du2, du3]],
                color='b', marker='x', linestyle='None', label="Online Update")
    ax.errorbar([1, 2, 3, 4], ds, [ns_to_s(t) for t in [dd0, dd1, dd2, dd3]],
                color='m', marker='x', linestyle='None', label="Disconnected Update")
    # Add text
    ax.text(0.05, ns_to_s(init), display_time(ns_to_s(init) * 1000), color="g")
    for i, v in enumerate([ns_to_s(t) for t in [b0, b1, b2, b3]]):
        ax.text(i + 1.05, v, display_time(v * 1000), color="r")
    for i, v in enumerate([ns_to_s(t) for t in [u0, u1, u2, u3]]):
        ax.text(i + 1.05, v, display_time(v * 1000), color="b")
    for i, v in enumerate([ns_to_s(t) for t in [d0, d1, d2, d3]]):
        ax.text(i + .55, v, display_time(v * 1000), color="m")


def repo_commit_deltas():
    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, sharex=True, sharey=True)
    fig.suptitle("Changes Since Last Commit Per Commit")
    ax1.set_title("Class Changes Per Project")
    ax2.set_title("Method Changes Per Project")
    ax3.set_title("Field Changes Per Project")

    x = [0, 1, 2, 3]
    classes = [
        [7, 8, 9, 13],  # Jackson
        [18, 32, 120, 34],  # Gremlin
        [3, 4, 11, 12]  # Neo4j
    ]
    methods = [
        [3, 1, 1, 10],  # Jackson
        [139, 29, 8, 11],  # Gremlin
        [4, 4, 22, 8],  # Neo4j
    ]
    fields = [
        [46, 49, 70, 98],  # Jackson
        [123, 191, 191, 197],  # Gremlin
        [13, 21, 108, 121],  # Neo4j
    ]
    plt.xticks(x, ["Commit 1", "Commit 2", "Commit 3", "Commit 4"])
    markers = ['o', 'o', 'o']
    projects = {'Jackson Databind': 'b', 'Gremlin Driver': 'g', 'Neo4j': 'r'}

    i = 0
    for p, col in projects.items():
        ax1.plot(x, classes[i], color=col, marker=markers[0])
        ax2.plot(x, methods[i], color=col, marker=markers[1])
        ax3.plot(x, fields[i], color=col, marker=markers[2])
        i += 1

    custom_lines = [Line2D([0], [0], color=projects['Jackson Databind'], lw=4, label='Jackson Databind'),
                    Line2D([0], [0], color=projects['Gremlin Driver'], lw=4, label='Gremlin Driver'),
                    Line2D([0], [0], color=projects['Neo4j'], lw=4, label='Neo4j')]
    # plt.legend(handles=custom_lines)
    fig.set_size_inches(9, 6)
    fig.text(0.5, 0.1, 'Commit', ha='center')
    fig.text(0.04, 0.5, 'Number of Changes Since Last Commit', va='center', rotation='vertical')
    plt.legend(loc="lower center", ncol=3, bbox_to_anchor=(-.05, -.8, 1.1, .01), mode="expand", handles=custom_lines)
    fig.subplots_adjust(bottom=0.18)
    fig.savefig("./results/project_deltas.pdf")


def avg_db_build_update(rs: List[Benchmark]):
    dbs = set([r.database for r in rs])
    fs = set([r.file_name for r in rs])

    # Map (File, Database) -> AVG
    avg_build = {}
    avg_update = {}
    avg_disupdt = {}
    for db in dbs:
        for f in fs:
            avg_build[(f, db)] = np.mean(
                [str_to_ns(r.total_time()) for r in rs if
                 r.file_name == f and r.database == db and ("BUILD" in r.phase or "INIT" in r.phase)])
            avg_update[(f, db)] = np.mean(
                [str_to_ns(r.total_time()) for r in rs if
                 r.file_name == f and r.database == db and "UPDATE" in r.phase])
            avg_disupdt[(f, db)] = np.mean(
                [str_to_ns(r.total_time()) for r in rs if
                 r.file_name == f and r.database == db and "DISCUPT" in r.phase])

    def plot_as_bars(xs: dict, type: str):
        data = {}
        for ((f, db), avg) in xs.items():
            if db not in data.keys():
                data[db] = [0, 0, 0]
            if "jackson" in f:
                data[db][0] = avg
            elif "gremlin" in f:
                data[db][1] = avg
            elif "neo4j" in f:
                data[db][2] = avg

        fig, ax = plt.subplots()
        ax.set_title("Average {} Time Per Database".format(type))
        ax.set_xlabel("Project")
        ax.set_ylabel("Time Elapsed (Logarithmic)")

        x = np.arange(3)
        i = 0.00
        max_avg = 0
        for (db, avg) in data.items():
            inc = 1.0 / len(data.items()) - 0.025
            ax.bar(x + i, avg, width=inc, label=db)
            for j, v in enumerate(avg):
                ax.text(j + i - 0.125, v, display_time(v * 1000), font=lbl_font)
            if max(avg) > max_avg:
                max_avg = max(avg)
            i += inc
        plt.yscale('log')
        plt.yticks([])
        plt.xticks([0.345, 1.345, 2.345],
                   ['jackson-databind', 'gremlin-driver', 'neo4j'])
        ymin, ymax = ax.get_ylim()
        plt.ylim([ymin, ymax + ymax * 0.10])
        plt.legend()
        fig.savefig("./results/db_{}_stats.pdf".format(type.replace(' ', '_').lower()))

    plot_as_bars(avg_update, "Online Update")
    plot_as_bars(avg_disupdt, "Disconnected Update")
    plot_as_bars(avg_build, "Build")


# TODO: Plot cache results
# TODO: Plot database reads/writes


def program_sizes():
    def plot_program_sizes(data, lbl):
        fig, ax = plt.subplots()
        ax.set_title("Initial Commit {} Code Statistics".format(lbl))
        ax.set_xlabel("GitHub Repository")
        ax.set_ylabel("Count")
        x = np.arange(3)
        ax.bar(x + 0.00, data[0], width=0.25, label="Classes")
        ax.bar(x + 0.25, data[1], width=0.25, label="Methods")
        ax.bar(x + 0.50, data[2], width=0.25, label="Fields")
        plt.xticks([0.125, 1.125, 2.125],
                   ['jackson-databind', 'gremlin-driver', 'neo4j'])
        for i, v in enumerate(data[0]):
            ax.text(i - 0.1, v + 100, str(v))
        for i, v in enumerate(data[1]):
            ax.text(i + 0.12, v + 100, str(v))
        for i, v in enumerate(data[2]):
            ax.text(i + 0.40, v + 100, str(v))
        plt.legend()
        plt.ylim([0, max(data[1]) + 600])
        fig.subplots_adjust(bottom=0.28)
        fig.savefig("./results/jar_{}_code_stats.pdf".format(lbl.lower().replace(" ", "_")))

    app_data = [
        # FasterXML/jackson-databind | apache/tinkerpop/gremlin-driver | neo4j/neo4j
        [701, 119, 40],  # Application Classes
        [7839, 1020, 491],  # Application Methods
        [2060, 519, 220]  # Application Fields
    ]
    lib_data = [
        [161, 133, 140],  # Library Classes
        [18106 - 7839, 9445 - 1020, 2680 - 491],  # Library Methods
        [2813, 2150, 363]  # Library Fields
    ]

    plot_program_sizes(app_data, "Project")
    plot_program_sizes(lib_data, "External Library")


def graph_sizes():
    fig, ax = plt.subplots()
    ax.set_title("Initial Commit Graph Statistics")
    ax.set_xlabel("GitHub Repository")
    ax.set_ylabel("Count")
    data = [
        # FasterXML/jackson-databind | apache/tinkerpop/gremlin-driver | neo4j/neo4j
        [555867, 132543, 42907],  # Vertices
        [3017313, 504138, 172136],  # Edges
    ]
    x = np.arange(3)
    vs = [d for d in data[0]]
    es = [d for d in data[1]]
    ax.bar(x + 0.00, vs, width=0.25, label="Vertices")
    ax.bar(x + 0.25, es, width=0.25, label="Edges")
    for i, v in enumerate(vs):
        ax.text(i - 0.15, v + 100000, str(v))
    for i, v in enumerate(es):
        ax.text(i + 0.10, v + 100000, str(v))
    plt.xticks([0.25, 1.25, 2.25],
               ['FasterXML/jackson-databind', 'apache/tinkerpop/gremlin-driver', 'neo4j/neo4j'],
               rotation=15)
    plt.legend()
    plt.ylim([0, max(es) + 300000])
    fig.savefig("./results/jar_graph_stats.pdf")


with open('./results/result.csv') as csv_file:
    csv_reader = csv.DictReader(csv_file, delimiter=',')
    results = []
    for row in csv_reader:
        results.append(Benchmark(
            plume_version=str(row["PLUME_VERSION"]),
            file_name=str(row["FILE_NAME"]),
            phase=str(row["PHASE"]),
            database=str(row["DATABASE"]),
            compiling_and_unpacking=int(row["COMPILING_AND_UNPACKING"]),
            soot=int(row["SOOT"]),
            program_structure=int(row["PROGRAM_STRUCTURE_BUILDING"]),
            base_cpg=int(row["BASE_CPG_BUILDING"]),
            db_write=int(row["DATABASE_WRITE"]),
            db_read=int(row["DATABASE_READ"]),
            df_pass=int(row["DATA_FLOW_PASS"]),
            cache_hits=int(row["CACHE_HITS"]),
            cache_misses=int(row["CACHE_MISSES"]),
            connect_deserialize=int(row["CONNECT_DESERIALIZE"]),
            disconnect_serialize=int(row["DISCONNECT_SERIALIZE"])
        ))

    results_per_db_jar = {}
    fs = set([x.file_name for x in results])
    dbs = set([x.database for x in results])
    for r in results:
        # Get (File, Database) -> [Results]
        if (r.file_name, r.database) not in results_per_db_jar.keys():
            results_per_db_jar[(r.file_name, r.database)] = [r]
        else:
            results_per_db_jar[(r.file_name, r.database)].append(r)
    # Plot stats of programs
    program_sizes()
    graph_sizes()
    repo_commit_deltas()
    # Plot results
    for f in fs:
        fig, ax = plt.subplots(nrows=len(dbs), ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * len(dbs)), tight_layout=False)
        # fig.set_size_inches(9, 8)
        # fig.subplots_adjust(bottom=0.2)
        i = 0
        for ((fname, db), r) in results_per_db_jar.items():
            if f == fname:
                update_build_perf(db, ax[i, 0], r)
                i += 1
                
        plt.xticks([0, 1, 2, 3, 4], ["Commit 0", "Commit 1", "Commit 2", "Commit 3", "Commit 4"])
        fig.suptitle(f, y=.95)
        fig.text(0.04, 0.5, 'Time Elapsed (s)', va='center', rotation='vertical')
        plt.legend(loc="lower center", ncol=4, bbox_to_anchor=(-.05, -0.45, 1.1, .102), mode="expand")
        fig.savefig("./results/build_updates_{}.pdf".format(f.split("/")[-1]))

    avg_db_build_update(results)
    plt.clf()
