#!/bin/bash

# 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
# Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

set -o errexit
set -o nounset
set -o pipefail

. /app/noel/hana/scripts/liblog.sh

info "*** starting hana! ***"
debug "   ===> Logback Configuration: ${HANA_LOGBACK_CONFIG_PATH:-unknown}"
debug "   ===> Dedicated Node:        ${WINTERFOX_DEDI_NODE:-unknown}"
debug "   ===> JVM Arguments:         ${HANA_JAVA_OPTS:-unknown}"

JAVA_OPTS=("-XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8")

if [[ -n "${HANA_LOGBACK_CONFIG_PATH:-}" && -f "${HANA_LOGBACK_CONFIG_PATH}" ]]; then
  JAVA_OPTS+=("-Dgay.floof.hana.logback.config=${HANA_LOGBACK_CONFIG_PATH}")
fi

if [[ -n "${WINTERFOX_DEDI_NODE:-}" ]]; then
  JAVA_OPTS+=("-Dwinterfox.dediNode=${WINTERFOX_DEDI_NODE}")
fi

if [[ -n "${HANA_JAVA_OPTS:-}" ]]; then
  JAVA_OPTS+=("$HANA_JAVA_OPTS")
fi

debug "Resolved JVM arguments: $JAVA_OPTS"
/app/noel/hana/bin/hana $@
