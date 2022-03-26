#!/bin/bash
export SBT_OPTS="-Xss1M -Xmx6G -XX:+UseG1GC"
sbt run