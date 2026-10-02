$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$services = @(
  @{ Name = 'HESTIA AUTH :3001'; Path = Join-Path $root 'backend\auth-service' },
  @{ Name = 'HESTIA PROPERTY :3002'; Path = Join-Path $root 'backend\property-service' },
  @{ Name = 'HESTIA INTERACTION :3003'; Path = Join-Path $root 'backend\interaction-service' },
  @{ Name = 'HESTIA GATEWAY :3000'; Path = Join-Path $root 'backend\gateway' }
)
foreach ($s in $services) {
  $cmd = "Set-Location '$($s.Path)'; if (-not (Test-Path node_modules)) { npm install }; npm start"
  Start-Process powershell -ArgumentList '-NoExit','-ExecutionPolicy','Bypass','-Command',$cmd -WindowStyle Normal
}
Write-Host 'HESTIA services opened in four PowerShell windows.'
