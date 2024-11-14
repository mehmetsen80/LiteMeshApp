##!/bin/sh
#
## wait-for.sh script to wait for services to be up
#host="$1"
#shift
#cmd="$@"
#
#until nc -z "$host" 80; do
#  >&2 echo "Service at $host is unavailable - sleeping"
#  sleep 2
#done
#
#>&2 echo "Service at $host is up - executing command"
#exec $cmd
