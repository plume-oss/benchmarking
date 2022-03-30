TIME_COLUMNS = ['COMPILING_AND_UNPACKING', 'SOOT', 'PROGRAM_STRUCTURE_BUILDING', 'BASE_CPG_BUILDING', 'DATA_FLOW_PASS']

ROOT_DIRECTORY = '../../results/'
RESULT_FILE = ROOT_DIRECTORY + 'result.csv'
CPG_MEMORY_FILE = ROOT_DIRECTORY + 'memory_results.csv'
TAINT_MEMORY_FILE = ROOT_DIRECTORY + 'memory_taint_results.csv'
STORAGE_FILE = ROOT_DIRECTORY + 'storage_results.csv'
TAINT_PERFORMANCE_FILE = ROOT_DIRECTORY + 'taint_results.csv'
TAINT_SEARCH_FILE = ROOT_DIRECTORY + 'taint_search_results.csv'
UPDATE_FILE = ROOT_DIRECTORY + 'update_cost.csv'
PLOT_ORDER = [
    "guava",
    "mybatis-3",
    "RxJava",
    "fastjson",
    "spring-boot",
    "jackson-core",
    "guice",
    "mockito",
    "scribejava",
]
