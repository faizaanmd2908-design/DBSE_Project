$ErrorActionPreference = 'Stop'
Write-Host 'Checking HESTIA Gateway...'
$health = Invoke-RestMethod 'http://localhost:3000/health'
$health | Format-List
Write-Host 'Checking properties...'
$props = Invoke-RestMethod 'http://localhost:3000/properties'
Write-Host "Properties returned: $($props.data.Count)"
if ($props.data.Count -lt 20) { throw 'Fewer than 20 properties returned.' }
Write-Host 'Checking real login + JWT...'
$body = @{ email='demo@hestia.com'; password='123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri 'http://localhost:3000/auth/login' -Method POST -ContentType 'application/json' -Body $body
if (-not $login.success -or [string]::IsNullOrWhiteSpace($login.data.token)) { throw 'Login/JWT check failed.' }
$token = $login.data.token
$me = Invoke-RestMethod -Uri 'http://localhost:3000/auth/me' -Method GET -Headers @{ Authorization = "Bearer $token" }
$me.data | Format-List
Write-Host 'HESTIA stack verification passed.'
