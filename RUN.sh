#!/bin/bash
export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=32G -Xmx32G"
sbt run