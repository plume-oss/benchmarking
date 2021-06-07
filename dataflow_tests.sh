#!/bin/bash
for i in {1..5}
do
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/n$i.bin",outFile=./results/df_neo4j.csv
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/g$i.bin",outFile=./results/df_gremlin.csv
   ../../joernio/joern/joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/j$i.bin",outFile=./results/df_jackson.csv
done
