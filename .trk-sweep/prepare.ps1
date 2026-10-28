$ErrorActionPreference = 'Stop'
$names = Get-Content "$PSScriptRoot/names.json" -Raw | ConvertFrom-Json
foreach ($name in $names) {
    $slug = $name -replace '[^a-zA-Z0-9]', ''
    $json = & "$PSScriptRoot/../oracle-assay/build/install/oracle-assay/bin/oracle-assay.bat" compile $name 2> "$PSScriptRoot/$slug.compile.log"
    if ($LASTEXITCODE -ne 0) { throw "Compile failed: $name" }
    $json | Set-Content "$PSScriptRoot/$slug.compile.json" -Encoding utf8
    $encoded = [Uri]::EscapeDataString($name)
    $card = Invoke-RestMethod "https://api.scryfall.com/cards/named?exact=$encoded&set=trk" -Headers @{ 'User-Agent' = 'ArgentumEngine/1.0'; 'Accept' = 'application/json' }
    $card | ConvertTo-Json -Depth 50 | Set-Content "$PSScriptRoot/$slug.scryfall.json" -Encoding utf8
    $rulings = Invoke-RestMethod $card.rulings_uri -Headers @{ 'User-Agent' = 'ArgentumEngine/1.0'; 'Accept' = 'application/json' }
    $rulings | ConvertTo-Json -Depth 30 | Set-Content "$PSScriptRoot/$slug.rulings.json" -Encoding utf8
    Write-Output "Prepared $name"
}
