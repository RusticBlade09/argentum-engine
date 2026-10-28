. "$PSScriptRoot/briefs.ps1"
$dest = "$PSScriptRoot/../mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards"
$lands = @('Andoria','Bajor','Cardassia Prime','Earth, Sector 001','Ferenginar','Orion',"Qo'noS",'Risa','Romulus','Vulcan')
foreach ($name in $lands) {
    $slug = $name -replace '[^a-zA-Z0-9]', ''
    $model = Get-Content "$PSScriptRoot/$slug.compile.json" -Raw | ConvertFrom-Json
    $code = Get-Content "$PSScriptRoot/$slug.skeleton.kt" -Raw
    $imports = @('com.wingedsheep.sdk.core.Color','com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Triggers','com.wingedsheep.sdk.scripting.AbilityCost','com.wingedsheep.sdk.scripting.EntersTapped','com.wingedsheep.sdk.scripting.TimingRule') | ForEach-Object { "import $_" }
    $code = $code.Replace('import com.wingedsheep.sdk.dsl.card', (($imports -join "`n") + "`nimport com.wingedsheep.sdk.dsl.card"))
    $abilities = @('replacementEffect(EntersTapped())', '', '    triggeredAbility {', '        trigger = Triggers.self.enters()', '        effect = Effects.GainLife(1)', '    }')
    foreach ($a in $model.script.activatedAbilities) {
        $abilities += @('', '    activatedAbility {', '        cost = AbilityCost.Tap', "        effect = Effects.AddMana(Color.$($a.effect.color))", '        manaAbility = true', '        timing = TimingRule.ManaAbility', '    }')
    }
    $code.Replace('// ABILITIES GO HERE', ($abilities -join "`n")) | Set-Content "$dest/$slug.kt" -Encoding utf8
}
$lines = @('package com.wingedsheep.mtg.sets.definitions.trk.cards','','import com.wingedsheep.sdk.dsl.basicLand','')
foreach ($name in @('Plains','Island','Swamp','Mountain','Forest')) {
    $c = Get-Content "$PSScriptRoot/$name.scryfall.json" -Raw | ConvertFrom-Json
    $lines += @("val Trk$name = basicLand($(Quote-Kotlin $name)) {", "    collectorNumber = $(Quote-Kotlin $c.collector_number)", "    artist = $(Quote-Kotlin $c.artist)", "    imageUri = $(Quote-Kotlin $c.image_uris.normal)", '}', '')
}
$lines | Set-Content "$dest/StarTrekBasicLands.kt" -Encoding utf8
