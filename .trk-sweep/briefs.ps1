$ErrorActionPreference = 'Stop'
function Quote-Kotlin([string]$s) { '"' + $s.Replace('\','\\').Replace('"','\"').Replace('$','\$').Replace("`r",'\r').Replace("`n",'\n').Replace("`t",'\t') + '"' }
Get-ChildItem "$PSScriptRoot/*.scryfall.json" | ForEach-Object {
    $c = Get-Content $_.FullName -Raw | ConvertFrom-Json
    $slug = $c.name -replace '[^a-zA-Z0-9]', ''
    $lines = @('package com.wingedsheep.mtg.sets.definitions.trk.cards', '', 'import com.wingedsheep.sdk.dsl.card', 'import com.wingedsheep.sdk.model.Rarity', '', "val $slug = card($(Quote-Kotlin $c.name)) {")
    if ($c.mana_cost) { $lines += "    manaCost = $(Quote-Kotlin $c.mana_cost)" }
    $lines += "    typeLine = $(Quote-Kotlin $c.type_line)"
    if ($c.color_identity.Count) { $lines += "    colorIdentity = $(Quote-Kotlin ($c.color_identity -join ''))" }
    $lines += "    oracleText = $(Quote-Kotlin $c.oracle_text)"
    if ($c.power -match '^\d+$') { $lines += "    power = $($c.power)" }
    if ($c.toughness -match '^\d+$') { $lines += "    toughness = $($c.toughness)" }
    $lines += @('    // ABILITIES GO HERE', '    metadata {', "        rarity = Rarity.$($c.rarity.ToUpper())", "        collectorNumber = $(Quote-Kotlin $c.collector_number)", "        artist = $(Quote-Kotlin $c.artist)")
    if ($c.flavor_text) { $lines += "        flavorText = $(Quote-Kotlin $c.flavor_text)" }
    $lines += @("        imageUri = $(Quote-Kotlin $c.image_uris.normal)", '    }', '}')
    $lines | Set-Content "$PSScriptRoot/$slug.skeleton.kt" -Encoding utf8
}
