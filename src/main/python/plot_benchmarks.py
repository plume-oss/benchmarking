import csv
import re
from typing import List

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.lines import Line2D

lbl_font = {
    'family': 'DejaVu Sans',
    'weight': 'normal',
    'size': 8
}

# Size in bytes
storage = {
    "TinkerGraph": {
        "jackson-databind": {"Program Size": 1510177, "Uncompressed": 230708501, "Compressed (tar.lz)": 35882737},
        "gremlin-driver": {"Program Size": 247808, "Uncompressed": 44098404, "Compressed (tar.lz)": 6692506},
        "neo4j": {"Program Size": 141312, "Uncompressed": 14532096, "Compressed (tar.lz)": 2230272}
    },
    "OverflowDB": {
        "jackson-databind": {"Program Size": 1510177, "Uncompressed": 47513600, "Compressed (tar.lz)": 24806855},
        "gremlin-driver": {"Program Size": 247808, "Uncompressed": 9121792, "Compressed (tar.lz)": 4615893},
        "neo4j": {"Program Size": 141312, "Uncompressed": 3035136, "Compressed (tar.lz)": 1661492}
    }
}

# Check memory with sudo pmap -d 668466|tail -n 1

remote_db = {
    # Initial
    # Storage: 195628
    # KAFKA | REST | GSQL | Zookeeper | GPE
    # (409 + 404 + 162) + 125 + 102 + 81.368 + 70.648
    "TigerGraph": {
        # KAFKA | GSQL | GSE | Zookeeper | GPE | RESTPP
        "jackson-databind": {"Initial Storage": 195632 * 1024, "Storage": 403456 * 1024,
                             "Initial Memory": ((409 + 404 + 162) + 125 + 102 + 81.368 + 70.648) * 1024 ** 2,
                             "Memory": ((1225 + 420 + 304) + 1145 + 772 + 335 + 190 + 168) * 1024 ** 2},
        # GSQL | KAFKA | GSE | GPE | RESTPP | Zookeeper
        "gremlin-driver": {"Initial Storage": 195632 * 1024, "Storage": 258768 * 1024,
                           "Initial Memory": ((409 + 404 + 162) + 125 + 102 + 81.368 + 70.648) * 1024 ** 2,
                           "Memory": (1462 + (998 + 421 + 301) + 732 + 121 + 169 + 349) * 1024 ** 2},
        # GSQL | KAFKA | GSE | Zookeeper | RESTPP | GPE
        "neo4j": {"Initial Storage": 195632 * 1024, "Storage": 236112 * 1024,
                  "Initial Memory": ((409 + 404 + 162) + 125 + 102 + 81.368 + 70.648) * 1024 ** 2,
                  "Memory": (1383 + (982 + 426 + 278) + 697 + 195 + 174 + 107) * 1024 ** 2}
    },
    # Neo4j base storage size = 513740
    # PID: 578724
    # Base memory: 2054820K
    "Neo4j": {
        # TODO Measure jackson storage again
        "jackson-databind": {
            "Initial Storage": 513740 * 1024,
            "Storage": 1619824 * 1024,
            "Initial Memory": 2054820 * 1024,
            "Memory": 5851804 * 1024
            },
        "gremlin-driver": {
            "Initial Storage": 513740 * 1024,
            "Storage": 564796 * 1024,
            "Initial Memory": 2054820 * 1024,
            "Memory": 3592587336
            },
        "neo4j": {
            "Initial Storage": 513740 * 1024,
            "Storage": 530456 * 1024,
            "Initial Memory": 2054820 * 1024,
            "Memory": 2154218136
            },
    },
    # For neptune, the initial and storage are swapped since the only metric is free local storage
    "Neptune": {
        "jackson-databind": {"Initial Storage": 8735408128, "Storage": 155460653056 - 155393799360,
                             "Memory": 19925262336 - 13653.88671875 * (1024 ** 2)},
        "gremlin-driver": {"Initial Storage": 8735408128, "Storage": 155471020032 - 155461988352,
                           "Memory": 19693785088 - 14311255754},
        "neo4j": {"Initial Storage": 8735408128, "Storage": 155481133056 - 155476795392,
                  "Memory": 19680649216 - 14407446528},
    },
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

storage_units = (
    ('Gb', 1024 ** 3),
    ('Mb', 1024 ** 2),
    ('kb', 1024),
    ('b', 1),
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


def display_storage(ms, granularity=2):
    result = []

    for name, count in storage_units:
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
        plt.xticks([0.25, 1.25, 2.25],
                   ['jackson-databind', 'gremlin-driver', 'neo4j'])
        for i, v in enumerate(data[0]):
            ax.text(i - 0.1, v + 100, str(v))
        for i, v in enumerate(data[1]):
            ax.text(i + 0.12, v + 100, str(v))
        for i, v in enumerate(data[2]):
            ax.text(i + 0.40, v + 100, str(v))
        plt.legend()
        plt.ylim([0, max(data[1]) + 600])
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
    plt.xticks([0.125, 1.125, 2.125],
               ['jackson-databind', 'gremlin-driver', 'neo4j'])
    plt.legend()
    plt.ylim([0, max(es) + 300000])
    fig.savefig("./results/jar_graph_stats.pdf")


def plot_build_updates(f):
    fig, ax = plt.subplots(nrows=len(dbs), ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * len(dbs)),
                           tight_layout=False)
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


def plot_cache_results(rs: List[Benchmark]):
    phases = ["BUILD", "UPDATE", "DISCUPT"]
    readable_phases = ["Full Build", "Online Update", "Disconnected Update"]

    fs = set([x.file_name for x in rs])
    # Plot per phase
    fig, axes = plt.subplots(nrows=len(phases), ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * len(dbs)),
                             tight_layout=False)
    i = 0
    x = np.arange(3)
    for j, p in enumerate(phases):
        avg_cache_hits = []
        avg_cache_misses = []
        for f in fs:
            avg_cache_hits.append(np.average([r.cache_hits for r in rs if r.file_name == f and p in r.phase]))
            avg_cache_misses.append(np.average([r.cache_misses for r in rs if r.file_name == f and p in r.phase]))
        ax = axes[i, 0]
        ax.set_title(readable_phases[j])

        perc_hits = np.array([])
        perc_misses = np.array([])
        for a in range(len(fs)):
            perc_hits = np.append(perc_hits, avg_cache_hits[a] / (avg_cache_hits[a] + avg_cache_misses[a]))
            perc_misses = np.append(perc_misses, avg_cache_misses[a] / (avg_cache_hits[a] + avg_cache_misses[a]))

        perc_hits = np.multiply(perc_hits, 100)
        perc_misses = np.multiply(perc_misses, 100)

        ax.bar(x + 0.00, perc_hits, width=0.25, color='tab:blue')
        ax.bar(x + 0.25, perc_misses, width=0.25, color='tab:orange')
        for k, v in enumerate(perc_hits):
            ax.text(k - 0.1, v, str("{:.2f}%".format(v)))
        for k, v in enumerate(perc_misses):
            ax.text(k + 0.15, v, str("{:.2f}%".format(v)))
        i += 1

    plt.xticks([0.125, 1.125, 2.125], fs)
    custom_lines = [Line2D([0], [0], color='tab:blue', lw=4, label='Cache Hits'),
                    Line2D([0], [0], color='tab:orange', lw=4, label='Cache Misses')]
    fig.suptitle("Cache Metrics Per Project", y=.95)
    fig.text(0.04, 0.5, 'Cache Operation Count (%)', va='center', rotation='vertical')
    fig.text(0.5, 0.06, 'GitHub Repository', ha='center')
    plt.legend(loc="lower center", ncol=2, bbox_to_anchor=(0.235, -0.4, .5, .102), mode="expand", handles=custom_lines)
    fig.savefig("./results/cache_metrics.pdf")


def plot_inmem_storage():
    fig, axes = plt.subplots(nrows=2, ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * 2),
                             tight_layout=False)
    fig.suptitle("In-Memory Database Storage Footprint")
    inmemdbs = ["TinkerGraph", "OverflowDB"]
    progs = ["jackson-databind", "gremlin-driver", "neo4j"]
    data = []
    for inmem in inmemdbs:
        inmem_storage = storage[inmem]
        p_data = []
        ucom_data = []
        com_data = []
        for p in progs:
            p_data.append(inmem_storage[p]["Program Size"])
            ucom_data.append(inmem_storage[p]["Uncompressed"])
            com_data.append(inmem_storage[p]["Compressed (tar.lz)"])
        data.append([p_data, ucom_data, com_data])

    x = np.arange(3)

    def plot_storage(ax, data, title):
        ax.set_title(title)
        ax.bar(x + 0.00, data[0], width=0.25, label="JAR Size")
        ax.bar(x + 0.25, data[1], width=0.25, label="Uncompressed CPG")
        ax.bar(x + 0.50, data[2], width=0.25, label="Compressed CPG (tar.lz)")
        ax.set_yscale('log')
        ymin, ymax = ax.get_ylim()
        ax.set_ylim([ymin, ymax * 1.5])
        ax.set_yticks([])
        for i, v in enumerate(data[0]):
            ax.text(i - 0.15, v + 10000, display_storage(v))
        for i, v in enumerate(data[1]):
            ax.text(i + 0.10, v + 10000, display_storage(v))
        for i, v in enumerate(data[2]):
            ax.text(i + 0.35, v + 10000, display_storage(v))

    fig.text(0.04, 0.5, 'Bytes (logarithmic)', va='center', rotation='vertical')
    fig.text(0.5, 0.1, 'GitHub Repository', ha='center')
    fig.subplots_adjust(bottom=0.18)
    for j, d in enumerate(inmemdbs):
        plot_storage(axes[j, 0], data[j], d)
    plt.legend(loc="lower center", ncol=3, bbox_to_anchor=(0, -.5, 1, .01), mode="expand")

    plt.xticks([0.25, 1.25, 2.25], progs)
    fig.savefig("./results/inmem_storage_footprint.pdf")


def plot_remote_storage():
    fig, axes = plt.subplots(nrows=3, ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * 3),
                             tight_layout=False)
    fig.suptitle("Remote Database Storage Footprint")
    remote_dbs = ["TigerGraph", "Neo4j", "Neptune"]
    progs = ["jackson-databind", "gremlin-driver", "neo4j"]
    data = []
    for remote in remote_dbs:
        remote_storage = remote_db[remote]
        initial = []
        used = []
        reserved = []
        for p in progs:
            if remote == "Neptune":
                used.append(remote_storage[p]["Storage"])
                reserved.append(remote_storage[p]["Initial Storage"] - remote_storage[p]["Storage"])
            else:
                initial.append(remote_storage[p]["Initial Storage"])
                used.append(remote_storage[p]["Storage"] - remote_storage[p]["Initial Storage"])
        data.append([initial, used, reserved])

    x = np.arange(3)

    def plot_storage(ax, data, title):
        ax.set_title(title)
        if title == "Neptune":
            for i, v in enumerate(data[2]):
                ax.text(i + 0.15, v, display_storage(v), color="tab:green")
            for i, v in enumerate(data[1]):
                ax.text(i + 0.15, v, display_storage(v), color="tab:orange")
        else:
            for i, v in enumerate(data[0]):
                ax.text(i + 0.15, v, display_storage(v), color="tab:blue")
            for i, v in enumerate(data[1]):
                ax.text(i - 0.15, v + data[0][i] + 10000, display_storage(v), color="tab:orange")
        # ax.set_yscale('log')
        ax.set_yticks([])
        if title == "Neptune":
            ax.bar(x, [0, 0, 0], width=0.25, label="Initial Size", color="tab:blue")
            ax.bar(x, data[1], width=0.25, label="Used Size", color="tab:orange")
            ax.bar(x, data[2], width=0.25, label="Reserved Size", color="tab:green", bottom=data[1])
        else:
            ax.bar(x, data[0], width=0.25, label="Initial size", color="tab:blue")
            ax.bar(x, data[1], width=0.25, label="Used Size", color="tab:orange", bottom=data[0])
            ax.bar(x, [0, 0, 0], width=0.25, label="Reserved Size", color="tab:green")

        ymin, ymax = ax.get_ylim()
        ax.set_ylim([ymin, ymax * 1.25])

    fig.text(0.05, 0.5, 'Bytes', va='center', rotation='vertical')
    fig.text(0.51, 0.1, 'GitHub Repository', ha='center')
    fig.subplots_adjust(bottom=0.2)
    for j, d in enumerate(remote_dbs):
        plot_storage(axes[j, 0], data[j], d)
    plt.legend(loc="lower center", ncol=3, bbox_to_anchor=(0, -0.9, 1, .01), mode="expand")
    # plt.legend()

    plt.xticks(x, progs)
    fig.savefig("./results/remote_storage_footprint.pdf")


def plot_remote_memory():
    fig, axes = plt.subplots(nrows=3, ncols=1, sharex=True, squeeze=False, figsize=(9, 2.5 * 3),
                             tight_layout=False)
    fig.suptitle("Remote Database Memory Footprint")
    remote_dbs = ["TigerGraph", "Neo4j", "Neptune"]
    progs = ["jackson-databind", "gremlin-driver", "neo4j"]
    data = []
    for remote in remote_dbs:
        remote_storage = remote_db[remote]
        initial = []
        # used = []
        for p in progs:
            initial.append(remote_storage[p]["Memory"])
            # used.append(remote_storage[p]["Storage"] - remote_storage[p]["Initial Storage"])
        data.append([initial])

    x = np.arange(3)

    def plot_memory(ax, data, title):
        ax.set_title(title)
        for i, v in enumerate(data[0]):
            ax.text(i + 0.15, v, display_storage(v), color="tab:blue")
        # for i, v in enumerate(data[1]):
        #     ax.text(i + 0.15, v, display_storage(v), color="tab:orange")
        ax.bar(x, data[0], width=0.25, label="Used size", color="tab:blue")
        # ax.bar(x, data[1], width=0.25, label="Used Size", color="tab:orange")
        ymin, ymax = ax.get_ylim()
        ax.set_ylim([ymin, ymax * 1.8])

    fig.text(0.05, 0.5, 'Bytes', va='center', rotation='vertical')
    fig.text(0.51, 0.03, 'GitHub Repository', ha='center')
    fig.subplots_adjust(bottom=0.1)
    for j, d in enumerate(remote_dbs):
        plot_memory(axes[j, 0], data[j], d)
    # plt.legend(loc="lower center", ncol=2, bbox_to_anchor=(0, -1, 1, .01), mode="expand")

    plt.xticks(x, progs)
    fig.savefig("./results/remote_memory_footprint.pdf")


tracer_files = {
    "OverflowDB": {
        "jackson-databind": "Memory_Maxes_OverflowDbDriver_jackson-databind.csv",
        "gremlin-driver": "Memory_Maxes_OverflowDbDriver_gremlin-driver.csv",
        "neo4j": "Memory_Maxes_OverflowDbDriver_neo4j.csv"
    },
    "TinkerGraph": {
        "jackson-databind": "Memory_Maxes_TinkerGraphDriver_jackson-databind.csv",
        "gremlin-driver": "Memory_Maxes_TinkerGraphDriver_gremlin-driver.csv",
        "neo4j": "Memory_Maxes_TinkerGraphDriver_neo4j.csv"
    },
    "Neo4j": {
        "jackson-databind": "Memory_Maxes_Neo4jDriver_jackson-databind.csv",
        "gremlin-driver": "Memory_Maxes_Neo4jDriver_gremlin-driver.csv",
        "neo4j": "Memory_Maxes_Neo4jDriver_neo4j.csv"
    },
    "Neptune": {
        "jackson-databind": "Tracer_Neptune_jackson.csv",
        "gremlin-driver": "Memory_Maxes_NeptuneDriver_gremlin-driver.csv",
        "neo4j": "Memory_Maxes_NeptuneDriver_neo4j.csv"
    },
    "TigerGraph": {
        "jackson-databind": "Tracer_TigerGraph_jackson.csv",
        "gremlin-driver": "Tracer_TigerGraph_gremlin.csv",
        "neo4j": "Tracer_TigerGraph_neo4j.csv"
    }
}


def plot_tracer_files():
    fig, axes = plt.subplots(nrows=len(tracer_files.keys()), ncols=1, sharex=True, squeeze=False,
                             figsize=(9, 2.5 * len(tracer_files.keys())),
                             tight_layout=False)
    fig.suptitle("Process Memory Footprint")

    def plot_memory(ax, x, y, e):
        ax.errorbar(x, y, e, linestyle="None", fmt='o')
        for i, v in enumerate(y):
            ax.text(i + 0.025, v + 10e7, display_storage(v))

    def extract_mem_use(trace_file):
        with open('./results/{}'.format(trace_file)) as csv_file:
            csv_reader = csv.DictReader(csv_file, delimiter=',')
            heap_entries = []
            use_entries = []
            for row in csv_reader:
                # heap_entries.append(int(''.join(re.findall('[0-9]+', str(row["Size [B]"])))))
                use_entries.append(int(''.join(re.findall('[0-9]+', str(row["Used [B]"])))))
        return (heap_entries, use_entries)

    fig.text(0.05, 0.5, 'Bytes', va='center', rotation='vertical')
    fig.text(0.51, 0.05, 'GitHub Repository', ha='center')
    fig.subplots_adjust(bottom=0.1)

    j = 0
    for d, f in tracer_files.items():
        current_ax = axes[j, 0]
        current_ax.set_title(d)
        _, use1 = extract_mem_use(f["jackson-databind"])
        _, use2 = extract_mem_use(f["gremlin-driver"])
        _, use3 = extract_mem_use(f["neo4j"])
        data = [np.average(use1), np.average(use2), np.average(use3)]
        dev = [np.std(use1), np.std(use2), np.std(use3)]
        plot_memory(current_ax, np.arange(3), data, dev)
        ymin, ymax = current_ax.get_ylim()
        current_ax.set_ylim([ymin, ymax * 1.25])

        j += 1

    plt.xticks(np.arange(3),
               ['jackson-databind', 'gremlin-driver', 'neo4j'])
    fig.savefig("./results/process_memory_footprint.pdf")


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
        plot_build_updates(f)
    plot_cache_results(results)
    avg_db_build_update(results)
    plot_inmem_storage()
    plot_remote_storage()
    plot_remote_memory()
    plot_tracer_files()
    plt.clf()
