$ErrorActionPreference = 'Stop'
$dest = "$PSScriptRoot/../mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards"
function Assert-Field($source, $field, $expected, $name) {
    $m = [regex]::Match($source, '\b' + $field + '\s*=\s*("(?:[^"\\]|\\.)*")')
    if (!$m.Success -or (($m.Groups[1].Value | ConvertFrom-Json) -cne $expected)) { throw "$name $field differs" }
}
foreach ($name in (Get-Content "$PSScriptRoot/reprint-names.txt")) {
    $slug = $name -replace '[^a-zA-Z0-9]', ''
    $c = Get-Content "$PSScriptRoot/$slug.scryfall.json" -Raw | ConvertFrom-Json
    $code = Get-Content "$dest/${slug}Reprint.kt" -Raw
    foreach ($pair in @(@('oracleId','oracle_id'),@('name','name'),@('collectorNumber','collector_number'),@('scryfallId','id'),@('artist','artist'),@('releaseDate','released_at'))) {
        Assert-Field $code $pair[0] $c.($pair[1]) $name
    }
    Assert-Field $code 'setCode' 'TRK' $name
    Assert-Field $code 'imageUri' $c.image_uris.normal $name
    if (!$code.Contains('Rarity.' + $c.rarity.ToUpper())) { throw "$name rarity differs" }
}
$basic = Get-Content "$dest/StarTrekBasicLands.kt" -Raw
foreach ($name in @('Plains','Island','Swamp','Mountain','Forest')) {
    $c = Get-Content "$PSScriptRoot/$name.scryfall.json" -Raw | ConvertFrom-Json
    $block = [regex]::Match($basic, 'val Trk'+$name+' = basicLand\("'+$name+'"\) \{([^}]+)\}').Groups[1].Value
    Assert-Field $block 'collectorNumber' $c.collector_number $name
    Assert-Field $block 'artist' $c.artist $name
    Assert-Field $block 'imageUri' $c.image_uris.normal $name
}
'All 11 reprint entries and 5 basic-land metadata blocks match their fetched TRK payloads.'
