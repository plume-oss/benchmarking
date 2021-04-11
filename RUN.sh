#!/bin/bash
export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -Xss2M -Xmx30G"
sbt run