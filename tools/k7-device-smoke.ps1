[CmdletBinding()]
param(
    [string]$Serial,
    [string]$OutputRoot = (Join-Path $env:TEMP "customiuizer-a13-device-evidence"),
    [ValidateRange(0, 180)]
    [int]$ObservationMinutes = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "tv.withaibuild.customiuizer.r13"
$adbCommand = Get-Command adb -ErrorAction Stop
$startedAt = Get-Date
$runName = "k7-{0}" -f $startedAt.ToString("yyyyMMdd-HHmmss")
$outputDirectory = Join-Path ([System.IO.Path]::GetFullPath($OutputRoot)) $runName
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function Invoke-AdbReadOnly {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $serialArguments = if ([string]::IsNullOrWhiteSpace($Serial)) {
        @()
    } else {
        @("-s", $Serial)
    }
    $output = & $adbCommand.Source @serialArguments @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "adb $($Arguments -join ' ') failed with exit code $exitCode.`n$($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Save-Lines {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [object[]]$Lines
    )

    $path = Join-Path $outputDirectory $Name
    @($Lines) | Set-Content -LiteralPath $path -Encoding utf8
    return $path
}

Write-Host "K7 read-only evidence collection"
Write-Host "Output: $outputDirectory"
Write-Host "This script does not clear logcat, install APKs, restart processes, or change device settings."

$devices = & $adbCommand.Source devices -l 2>&1
Save-Lines -Name "adb-devices.txt" -Lines $devices | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "adb devices failed."
}

$connectedSerials = @(
    $devices |
        Select-String -Pattern "^(\S+)\s+device(?:\s|$)" |
        ForEach-Object { $_.Matches[0].Groups[1].Value }
)
if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($connectedSerials.Count -ne 1) {
        throw "Expected exactly one authorized device; found $($connectedSerials.Count). Use -Serial when multiple devices are connected."
    }
    $Serial = $connectedSerials[0]
} elseif ($Serial -notin $connectedSerials) {
    throw "Device '$Serial' is not connected and authorized."
}

$properties = [ordered]@{
    serial = $Serial
    model = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.product.model")) -join " "
    product = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.product.device")) -join " "
    android = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.build.version.release")) -join " "
    sdk = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.build.version.sdk")) -join " "
    miui = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.miui.ui.version.name") -AllowFailure) -join " "
    build = (Invoke-AdbReadOnly -Arguments @("shell", "getprop", "ro.build.fingerprint")) -join " "
}
$properties.GetEnumerator() |
    ForEach-Object { "{0}={1}" -f $_.Key, $_.Value } |
    Set-Content -LiteralPath (Join-Path $outputDirectory "device-properties.txt") -Encoding utf8

$packageDump = Invoke-AdbReadOnly -Arguments @("shell", "dumpsys", "package", $packageName) -AllowFailure
$packageSummary = @(
    $packageDump |
        Select-String -Pattern "versionCode=|versionName=|firstInstallTime=|lastUpdateTime=" |
        ForEach-Object { $_.Line.Trim() }
)
if ($packageSummary.Count -eq 0) {
    $packageSummary = @("Package not found or package metadata was unavailable: $packageName")
}
Save-Lines -Name "package-version.txt" -Lines $packageSummary | Out-Null

function Capture-Pids {
    param([string]$Name)

    $rows = foreach ($processName in @("system_server", "com.android.systemui", "com.miui.home", $packageName)) {
        $pid = (Invoke-AdbReadOnly -Arguments @("shell", "pidof", $processName) -AllowFailure) -join " "
        "{0}={1}" -f $processName, $pid.Trim()
    }
    Save-Lines -Name $Name -Lines $rows | Out-Null
}

Capture-Pids -Name "pids-start.txt"
Save-Lines -Name "collection-start.txt" -Lines @(
    "host=$($startedAt.ToString("o"))",
    "device=$((Invoke-AdbReadOnly -Arguments @("shell", "date", "+%Y-%m-%dT%H:%M:%S%z") -AllowFailure) -join " ")"
) | Out-Null

if ($ObservationMinutes -gt 0) {
    Write-Host "Observation window started for $ObservationMinutes minute(s). Perform the checklist manually on the device."
    Start-Sleep -Seconds ($ObservationMinutes * 60)
} else {
    Write-Host "No timed observation requested. Perform the checklist manually, then rerun this script to capture a later snapshot if needed."
}

$logcat = Invoke-AdbReadOnly -Arguments @("logcat", "-d", "-v", "threadtime") -AllowFailure
Save-Lines -Name "logcat.txt" -Lines $logcat | Out-Null

$moduleLog = @(
    $logcat |
        Select-String -Pattern "CustoMIUIzer|LSPosed|LSPHooker|Vector|tv\.withaibuild\.customiuizer\.r13" |
        ForEach-Object { $_.Line }
)
Save-Lines -Name "module-load-filtered.txt" -Lines $moduleLog | Out-Null

$failureSummary = @(
    $logcat |
        Select-String -Pattern "FATAL EXCEPTION|Fatal signal|ANR in|am_anr|Watchdog|watchdog|system_server.*(crash|died)|com\.android\.systemui.*(crash|died)|com\.miui\.home.*(crash|died)" |
        ForEach-Object { $_.Line }
)
Save-Lines -Name "crash-anr-watchdog-summary.txt" -Lines $failureSummary | Out-Null

Capture-Pids -Name "pids-end.txt"
$endedAt = Get-Date
Save-Lines -Name "collection-end.txt" -Lines @(
    "host=$($endedAt.ToString("o"))",
    "device=$((Invoke-AdbReadOnly -Arguments @("shell", "date", "+%Y-%m-%dT%H:%M:%S%z") -AllowFailure) -join " ")",
    "elapsed=$($endedAt - $startedAt)"
) | Out-Null

Write-Host "Read-only evidence collection complete: $outputDirectory"
Write-Host "Review module-load-filtered.txt and crash-anr-watchdog-summary.txt; do not treat package-name-only matches as proof of a module fault."
