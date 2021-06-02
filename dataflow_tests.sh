#!/bin/bash
for i in {1..5}
do
   ./joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/n$i.bin",outFile=neo4j.csv
   ./joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/g$i.bin",outFile=gremlin.csv
   ./joern --script src/main/cpgql/DataFlowQueries.sc --params cpgFile="./src/main/resources/cpgs/j$i.bin",outFile=jackson.csv
done
