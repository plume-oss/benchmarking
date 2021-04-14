import csv
from typing import List

import matplotlib.pyplot as plt
import numpy as np


class Benchmark:
    def __init__(self, plume_version, file_name, phase, database, compiling_and_unpacking, soot, program_structure,
                 base_cpg, db_read, db_write, df_pass):
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


def ns_to_s(ns):
    return ns * 10e-9


def update_build_perf(f: str, db: str, rs: List[Benchmark]):
    fig, ax = plt.subplots()
    ax.set_title("{}: {}".format(db, f))
    ax.set_xlabel("Code Increments")
    ax.set_ylabel("Time Elapsed (s)")
    init, init_std = avg(rs, "INITIAL"), stdd(rs, "INITIAL")
    b0, db0 = avg(rs, "BUILD0"), stdd(rs, "BUILD0")
    b1, db1 = avg(rs, "BUILD1"), stdd(rs, "BUILD1")
    b2, db2 = avg(rs, "BUILD2"), stdd(rs, "BUILD2")
    b3, db3 = avg(rs, "BUILD3"), stdd(rs, "BUILD3")
    u0, du0 = avg(rs, "UPDATE0"), stdd(rs, "UPDATE0")
    u1, du1 = avg(rs, "UPDATE1"), stdd(rs, "UPDATE1")
    u2, du2 = avg(rs, "UPDATE2"), stdd(rs, "UPDATE2")
    u3, du3 = avg(rs, "UPDATE3"), stdd(rs, "UPDATE3")
    plt.xticks([])
    ax.errorbar([0], [ns_to_s(t) for t in [init]], [ns_to_s(t) for t in [init_std]], color='g', marker='o',
                linestyle='None', label="Initial Build")
    ax.errorbar([1, 2, 3, 4], [ns_to_s(t) for t in [b0, b1, b2, b3]], [ns_to_s(t) for t in [db0, db1, db2, db3]],
                color='r', marker='x', linestyle='None', label="Full Build")
    ax.errorbar([1, 2, 3, 4], [ns_to_s(t) for t in [u0, u1, u2, u3]], [ns_to_s(t) for t in [du0, du1, du2, du3]],
                color='b', marker='x', linestyle='None', label="Incremental Update")
    plt.legend()
    fig.savefig("./results/Plume_BUILD_UPDATE_{}_{}.pdf".format(f.replace('/', '_'), db))


# TODO: Plot average update/build against each database
# TODO: Plot cache results
# TODO: Plot database reads/writes
# TODO: Plot vertices/edges of each program

def program_sizes():
    fig, ax = plt.subplots()
    ax.set_title("Initial Program Code Statistics")
    ax.set_xlabel("Codebase")
    ax.set_ylabel("Count")
    data = [
        # FasterXML/jackson-databind | apache/tinkerpop/gremlin-driver | neo4j/neo4j
        [701, 119, 40],  # Application Classes
        [161, 133, 140],  # Library Classes
        [7839, 1020, 491],  # Application Methods
        [18106 - 7839, 9445 - 1020, 2680 - 491],  # Library Methods
        [2060, 519, 220],  # Application Fields
        [2813, 2150, 363]  # Library Fields
    ]
    x = np.arange(3)
    ax.bar(x + 0.00, data[0], width=0.25, label="Application Classes")
    ax.bar(x + 0.00, data[2], width=0.25, label="Application Methods")
    ax.bar(x + 0.00, data[4], width=0.25, label="Application Fields")
    ax.bar(x + 0.25, data[1], width=0.25, label="Library Classes")
    ax.bar(x + 0.25, data[3], width=0.25, label="Library Methods")
    ax.bar(x + 0.25, data[5], width=0.25, label="Library Fields")
    plt.xticks([0.125, 1.125, 2.125],
               ['FasterXML/jackson-databind', 'apache/tinkerpop/gremlin-driver', 'neo4j/neo4j'],
               rotation=15)
    plt.legend()
    fig.subplots_adjust(bottom=0.28)
    fig.savefig("./results/Plume_JAR_CODE_STATS.pdf")


def graph_sizes():
    fig, ax = plt.subplots()
    ax.set_title("Initial Program Graph Statistics")
    ax.set_xlabel("Codebase")
    ax.set_ylabel("Count (thousands)")
    data = [
        # FasterXML/jackson-databind | apache/tinkerpop/gremlin-driver | neo4j/neo4j
        [752261, 256898, 61251],  # Vertices
        [3829346, 778624, 222928],  # Edges
    ]
    x = np.arange(3)
    ax.bar(x + 0.00, [d / 1000 for d in data[0]], width=0.25, label="Vertices")
    ax.bar(x + 0.25, [d / 1000 for d in data[1]], width=0.25, label="Edges")
    plt.xticks([0.25, 1.25, 2.25],
               ['FasterXML/jackson-databind', 'apache/tinkerpop/gremlin-driver', 'neo4j/neo4j'],
               rotation=15)
    plt.legend()
    fig.subplots_adjust(bottom=0.28)
    fig.savefig("./results/Plume_JAR_GRAPH_STATS.pdf")


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
            df_pass=int(row["DATA_FLOW_PASS"])
        ))

    results_per_db_jar = {}
    for r in results:
        if (r.file_name, r.database) not in results_per_db_jar.keys():
            results_per_db_jar[(r.file_name, r.database)] = [r]
        else:
            results_per_db_jar[(r.file_name, r.database)].append(r)
    # Plot stats of programs
    program_sizes()
    graph_sizes()
    # Plot results
    for ((f, db), r) in results_per_db_jar.items():
        update_build_perf(f, db, r)
    plt.clf()
