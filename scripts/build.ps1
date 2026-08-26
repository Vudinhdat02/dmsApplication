# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Vudinhdat02
[CmdletBinding()]
param([switch]$Release)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    dotnet restore "DMSServer/DMSbackend.slnx"
    dotnet build "DMSServer/DMSbackend.slnx" --configuration Release --no-restore
    if ($Release) { & ".\gradlew.bat" clean assembleRelease }
    else { & ".\gradlew.bat" clean assembleDebug }
} finally { Pop-Location }
