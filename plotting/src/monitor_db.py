import os.path
import sys
from subprocess import *
import time
import re
from datetime import datetime

dbs = ["NEO", "TG"]
selected_db = "NEO"

if len(sys.argv) < 2:
    print("Database name as argument required i.e. {}".format(dbs))
elif sys.argv[1] not in dbs:
    print("Valid args are {}".format(dbs))
    exit(1)
else:
    selected_db = sys.argv[1]

OUT_FILE = './results/db_mem_storage_results.csv'
init_headers = False

if os.path.isfile(OUT_FILE) == False:
    init_headers = True

print("Recording memory and storage for {}".format(selected_db))
print("Use Ctrl+C to stop monitoring")
while True:
    with open(OUT_FILE, 'a') as f:
        if init_headers is True:
            f.write('DATE,DATABASE,MEMORY,STORAGE\n')
            init_headers = False

        if selected_db == "NEO":
            # Get storage
            proc = Popen(["sudo", "du", "/var/lib/docker/volumes/docker_neo4j-data"], stdout=PIPE).communicate()[0]
            storage = "".join(map(chr, proc)).split('\n')[-2].split()[0]
            # Get memory
            proc = Popen(["docker", "exec", "-it", "neo4j-plume-benchmark", "pidof", "java"], stdout=PIPE).communicate()[0]
            pid = "".join(map(chr, proc)).strip()
            proc = Popen(["docker", "exec", "-it", "neo4j-plume-benchmark", "pmap", pid], stdout=PIPE).communicate()[0]
            memory = "".join(map(chr, proc)).strip().split('\n')[-1].split()[1]
            memory =  re.sub('\D', '', memory)
            # Do time
            f.write('{},{},{},{}\n'.format(datetime.now(), 'Neo4j', memory, storage))
        elif selected_db == "TG":
            # Get storage
            proc = Popen(["sudo", "du", "/tmp/plume/tigergraph-data"], stdout=PIPE).communicate()[0]
            storage = "".join(map(chr, proc)).split('\n')[-2].split()[0]
            # Get memory
            pids = []
            proc = Popen(["docker", "exec", "-it", "tigergraph-plume-benchmark", "pidof", "java"], stdout=PIPE).communicate()[0]
            pids += "".join(map(chr, proc)).strip().split()
            proc = Popen(["docker", "exec", "-it", "tigergraph-plume-benchmark", "pidof", "nginx"], stdout=PIPE).communicate()[0]
            pids += "".join(map(chr, proc)).strip().split()
            proc = Popen(["docker", "exec", "-it", "tigergraph-plume-benchmark", "pgrep", "tg_*"], stdout=PIPE).communicate()[0]
            pids += "".join(map(chr, proc)).strip().split()
            memory = 0
            for pid in pids:
                proc = Popen(["docker", "exec", "-itu", "tigergraph", "tigergraph-plume-benchmark",  "pmap", pid], stdout=PIPE).communicate()[0]
                memory_str = "".join(map(chr, proc)).strip().split('\n')[-1].split()[1]
                memory += int(re.sub('\D', '', memory_str))
            # Do time
            f.write('{},{},{},{}\n'.format(datetime.now(), 'TigerGraph', memory, storage))
    time.sleep(5)