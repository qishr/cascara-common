#!/bin/bash
# rm -rf .classpath .project .settings
# rm -rf */.classpath */.project */.settings
find . -name ".project" -o -name ".classpath" -o -name ".settings" | xargs rm -rf
rm -rf bin build */bin */build