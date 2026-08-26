#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Vudinhdat02
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"
dotnet restore DMSServer/DMSbackend.slnx
dotnet build DMSServer/DMSbackend.slnx --configuration Release --no-restore
./gradlew clean assembleDebug
