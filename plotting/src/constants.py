TIME_COLUMNS = ['COMPILING_AND_UNPACKING', 'SOOT', 'PROGRAM_STRUCTURE_BUILDING', 'BASE_CPG_BUILDING', 'DATA_FLOW_PASS']

ROOT_DIRECTORY = '../../results/'
RESULT_FILE = ROOT_DIRECTORY + 'result.csv'
MEMORY_FILE = ROOT_DIRECTORY + 'memory_results.csv'
STORAGE_FILE = ROOT_DIRECTORY + 'storage_results.csv'
TAINT_FILE = ROOT_DIRECTORY + 'taint_results.csv'
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
