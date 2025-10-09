#!/usr/bin/env bash
# wait-for-it.sh - Wait for a host:port to be available

TIMEOUT=3
QUIET=0

echoerr() { if [ "$QUIET" -ne 1 ]; then echo "$@" 1>&2; fi; }

usage() {
    echo "Usage: $0 host:port [-t timeout] [-- command args]"
    exit 1
}

wait_for() {
    local host=$1
    local port=$2
    local timeout=$3
    local start_ts=$(date +%s)

    while :
    do
        nc -z "$host" "$port" >/dev/null 2>&1
        result=$?
        if [ $result -eq 0 ]; then
            break
        fi
        now_ts=$(date +%s)
        if [ $((now_ts - start_ts)) -ge $timeout ]; then
            echo "Timeout occurred after waiting $timeout seconds for $host:$port"
            return 1
        fi
        sleep 1
    done
    return 0
}

# parse arguments
while [ $# -gt 0 ]
do
    case "$1" in
        -q|--quiet)
            QUIET=1
            shift
            ;;
        -t)
            TIMEOUT="$2"
            shift 2
            ;;
        --)
            shift
            break
            ;;
        *)
            break
            ;;
    esac
done

if [ $# -lt 1 ]; then
    usage
fi

# Split host:port safely
IFS=":" read -r HOST PORT <<< "$1"
shift

wait_for "$HOST" "$PORT" "$TIMEOUT"
result=$?

if [ $# -gt 0 ]; then
    exec "$@"
else
    exit $result
fi
