import os.path
import sys
from subprocess import *
import time

dbs = ["NEO", "TG"]
selected_db = "NEO"

if len(sys.argv) < 2:
    print("Database name as argument required i.e. {}".format(dbs))
elif sys.argv[1] not in dbs:
    print("Valid args are {}".format(dbs))
else:
    selected_db = sys.argv[1]

MEM_FILE = './results/db_memory_results.csv'
init_headers = False

if not os.path.isfile(MEM_FILE):
    init_headers = True

with open('./results/db_memory.csv', 'a') as f:
    print("Recording memory and storage for {}".format(selected_db))
    print("Use Ctrl+C to stop monitoring")
    if init_headers is True:
            f.write('DATE,DATABASE,DATA\n')
    while True:
        if selected_db == "NEO":
            proc = Popen(["sudo", "du", "/var/lib/docker/volumes/docker_neo4j-data"], stdout=PIPE).communicate()[-1]
            print(proc)
        elif selected_db == "TG":
            pass
        time.sleep(500)