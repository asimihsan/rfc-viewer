#!/usr/bin/env bash

set -euo pipefail

poetry install
poetry run python -m spacy download en_core_web_trf
