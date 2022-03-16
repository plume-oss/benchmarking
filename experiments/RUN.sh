#!/bin/bash
export SBT_OPTS="-Xss1M -Xmx15G -XX:+UseG1GC"
sbt run