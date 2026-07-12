#!/bin/bash
MY_ROOT=$(dirname "${0}")

podman build -t cautious-carnival:dev -v ~/.m2:/root/.m2:Z "${MY_ROOT}"
