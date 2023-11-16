#!/bin/bash

export CLASSPATH=`dirname $0`:${CLASSPATH}

java MathConverter $*
