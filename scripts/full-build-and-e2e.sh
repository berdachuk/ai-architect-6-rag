#!/usr/bin/env bash
# Full DocuRAG reactor build + E2E (Docker Compose Postgres + fat JAR started by tests).
#
# Usage:
#   ./scripts/full-build-and-e2e.sh              # clean verify; Compose teardown without -v from tests only
#   ./scripts/full-build-and-e2e.sh --teardown-volumes
#       Before: docker compose down -v (clean slate).
#       During tests: JVM teardown uses down -v when Maven profile e2e-teardown-volumes is active (docu-rag-e2e).
#       After: docker compose down -v again (remove DB volumes).
#
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_DIR="${REPO_ROOT}/docu-rag"
E2E_TARGET="${REPO_ROOT}/docu-rag-e2e/target"
E2E_CUCUMBER="${REPO_ROOT}/docu-rag-e2e/build/cucumber-reports"

TEARDOWN_VOLUMES=false
for arg in "$@"; do
  case "$arg" in
    --teardown-volumes) TEARDOWN_VOLUMES=true ;;
    -h|--help)
      echo "Usage: $0 [--teardown-volumes]"
      echo "  --teardown-volumes  Run 'docker compose down -v' before and after mvn; pass volume removal to E2E JVM."
      exit 0
      ;;
  esac
done

compose_down_v() {
  echo ">>> Compose down (remove volumes) in ${COMPOSE_DIR}"
  if [[ -d "${COMPOSE_DIR}" ]]; then
    (cd "${COMPOSE_DIR}" && docker compose down --remove-orphans -v 2>/dev/null) || true
    (cd "${COMPOSE_DIR}" && docker-compose down --remove-orphans -v 2>/dev/null) || true
  fi
}

print_e2e_report() {
  echo ""
  echo "================ E2E / TEST REPORTS ================"
  local sf="${E2E_TARGET}/surefire-reports"
  if [[ -d "${sf}" ]]; then
    echo "--- Surefire (${sf}) ---"
    find "${sf}" -maxdepth 1 -type f \( -name '*.txt' -o -name '*.xml' \) -print 2>/dev/null | sort | while read -r f; do
      echo "  $(basename "$f")"
    done
    local summary
    summary="$(find "${sf}" -maxdepth 1 -name '*.txt' -print 2>/dev/null | head -1)"
    if [[ -n "${summary}" && -f "${summary}" ]]; then
      echo "--- First Surefire summary (tail) ---"
      tail -n 40 "${summary}"
    fi
  else
    echo "No surefire-reports at ${sf} (build may have failed early)."
  fi

  if [[ -f "${E2E_CUCUMBER}/e2e-report.html" ]]; then
    echo "--- Cucumber HTML ---"
    echo "  file://${E2E_CUCUMBER}/e2e-report.html"
  fi
  if [[ -f "${E2E_CUCUMBER}/cucumber.xml" ]]; then
    echo "--- Cucumber JUnit XML ---"
    echo "  ${E2E_CUCUMBER}/cucumber.xml"
  fi
  local clog="${REPO_ROOT}/docu-rag-e2e/target/docurag-e2e-compose.log"
  local dlog="${REPO_ROOT}/docu-rag-e2e/target/docurag-e2e-docker-info.log"
  if [[ -f "${clog}" ]]; then
    echo "--- Docker compose log (tail) ---"
    tail -n 25 "${clog}" 2>/dev/null || true
  fi
  if [[ -f "${dlog}" ]]; then
    echo "--- docker info log (tail) ---"
    tail -n 15 "${dlog}" 2>/dev/null || true
  fi
  echo "======================================================"
}

if [[ "${TEARDOWN_VOLUMES}" == true ]]; then
  compose_down_v
fi

EXTRA_PROPS=()
if [[ "${TEARDOWN_VOLUMES}" == true ]]; then
  EXTRA_PROPS+=("-Pe2e-teardown-volumes")
fi

echo ">>> mvn clean verify (repo root) ${EXTRA_PROPS[*]:-}"
set +e
cd "${REPO_ROOT}"
mvn clean verify "${EXTRA_PROPS[@]}"
MVN_EXIT=$?
set -e

print_e2e_report

if [[ "${TEARDOWN_VOLUMES}" == true ]]; then
  compose_down_v
fi

echo ""
if [[ "${MVN_EXIT}" -eq 0 ]]; then
  echo "STATUS: SUCCESS (mvn exit 0)"
else
  echo "STATUS: FAILED (mvn exit ${MVN_EXIT}) - fix failures above and re-run."
fi

exit "${MVN_EXIT}"
