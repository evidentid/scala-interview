#!/usr/bin/env bash
set -euo pipefail

# Transfer into real entry point
docker-entrypoint.sh $@ -c fsync=off -c synchronous_commit=off -c full_page_writes=off -c max_connections=300
