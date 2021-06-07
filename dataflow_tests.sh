#!/bin/bash
for i in {1..5}
do
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/n$i.bin",outFile=./results/dataflow_queries.csv
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/g$i.bin",outFile=./results/dataflow_queries.csv
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/j$i.bin",outFile=./results/dataflow_queries.csv
done
