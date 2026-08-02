<#
.SYNOPSIS
    Verifies the A13 signing configuration without exposing secrets.

.DESCRIPTION
    Checks that the A13 signing configuration is supplied exactly through the
    Gradle property customiuizerA13KeystoreProperties or the environment variable
    CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES, and that the referenced keystore
    properties file and storeFile exist.

    This script does not print passwords or keystore secret content.
#>

[CmdletBinding()]
param(
    [switch]$PrintHelp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-UserGradlePropertiesPath {
    $homeDir = [Environment]::GetFolderPath([Environment+SpecialFolder]::UserProfile)
    return Join-Path (Join-Path $homeDir ".gradle") "gradle.properties"
}

function Get-GradlePropertyValue {
    param(
        [string]$PropertiesPath,
        [string]$PropertyName
    )

    if (-not (Test-Path -LiteralPath $PropertiesPath -PathType Leaf)) {
        return $null
    }

    $lines = Get-Content -LiteralPath $PropertiesPath
    foreach ($line in $lines) {
        if ($line -match "^\s*" + [regex]::Escape($PropertyName) + "\s*=\s*(.+?)\s*$") {
            return $Matches[1].Trim()
        }
    }
    return $null
}

function Test-RequiredProperty {
    param(
        [hashtable]$Properties,
        [string]$Key
    )

    $value = $Properties[$Key]
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $false
    }
    return $true
}

if ($PrintHelp) {
    Write-Host "Usage: .\scripts\check-signing-config.ps1"
    exit 0
}

$gradlePropName = "customiuizerA13KeystoreProperties"
$envVarName = "CUSTOMIUIZER_A13_KEYSTORE_PROPERTIES"

$configuredPath = $null
$source = $null

# 1. Environment variable
$envValue = [Environment]::GetEnvironmentVariable($envVarName)
if (-not [string]::IsNullOrWhiteSpace($envValue)) {
    $configuredPath = $envValue
    $source = "environment variable"
}

# 2. Windows user Gradle properties
if ([string]::IsNullOrWhiteSpace($configuredPath)) {
    $userPropertiesPath = Get-UserGradlePropertiesPath
    $gradleValue = Get-GradlePropertyValue -PropertiesPath $userPropertiesPath -PropertyName $gradlePropName
    if (-not [string]::IsNullOrWhiteSpace($gradleValue)) {
        $configuredPath = $gradleValue
        $source = "user Gradle properties"
    }
}

Write-Host "Signing discovery mode: EXACT_CONFIG_ONLY"
Write-Host "Expected property: $gradlePropName"
Write-Host "Expected environment variable: $envVarName"

if ([string]::IsNullOrWhiteSpace($configuredPath)) {
    Write-Host "Configuration state: NOT_CONFIGURED"
    Write-Host "Properties file path: (none)"
    Write-Host "Properties file exists: false"
    Write-Host "Signing enabled: false"
    Write-Host "`nA13 signing configuration is not present. Debug builds and tests can still run."
    exit 0
}

Write-Host "Configuration source: $source"
Write-Host "Properties file path: $configuredPath"

$propertiesFileExists = Test-Path -LiteralPath $configuredPath -PathType Leaf
Write-Host "Properties file exists: $propertiesFileExists"

if (-not $propertiesFileExists) {
    Write-Host "Signing enabled: false"
    Write-Error "The configured properties file does not exist: $configuredPath"
    exit 1
}

$properties = @{}
foreach ($line in (Get-Content -LiteralPath $configuredPath)) {
    if ($line -match "^\s*([^#\s=]+)\s*=\s*(.*?)\s*$") {
        $properties[$Matches[1]] = $Matches[2]
    }
}

$requiredKeys = @("storeFile", "storePassword", "keyAlias", "keyPassword")
$missingKeys = $requiredKeys | Where-Object { -not (Test-RequiredProperty -Properties $properties -Key $_) }

Write-Host "Required fields present: $(($requiredKeys | Where-Object { $_ -notin $missingKeys }) -join ', ')"
if ($missingKeys) {
    Write-Host "Required fields missing: $($missingKeys -join ', ')"
    Write-Host "Signing enabled: false"
    Write-Error "The configured properties file is missing required fields."
    exit 1
}

$storeFilePath = $properties["storeFile"]
Write-Host "Store file path: $storeFilePath"

$storeFileExists = Test-Path -LiteralPath $storeFilePath -PathType Leaf
Write-Host "Store file exists: $storeFileExists"

if (-not $storeFileExists) {
    Write-Host "Signing enabled: false"
    Write-Error "The keystore file referenced by storeFile does not exist: $storeFilePath"
    exit 1
}

Write-Host "Signing enabled: true"
exit 0
