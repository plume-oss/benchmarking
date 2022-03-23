#!/bin/bash
export SBT_OPTS="-Xss1M -Xmx8G -XX:+UseG1GC"
sbt run