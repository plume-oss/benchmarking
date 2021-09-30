import plot_memory

ROOT_DIRECTORY = './results/'
RESULT_FILE = ROOT_DIRECTORY + 'result.csv'
MEMORY_FILE = ROOT_DIRECTORY + 'memory_results.csv'

if __name__ == '__main__':
    plot_memory.plot(MEMORY_FILE)
