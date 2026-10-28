$dest = "$PSScriptRoot/../mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards"
function Write-Card($slug, $imports, $abilities) {
    $code = Get-Content "$PSScriptRoot/$slug.skeleton.kt" -Raw
    $code = $code.Replace('import com.wingedsheep.sdk.dsl.card', (($imports | ForEach-Object { "import $_" }) -join "`n") + "`nimport com.wingedsheep.sdk.dsl.card")
    $code.Replace('// ABILITIES GO HERE', $abilities) | Set-Content "$dest/$slug.kt" -Encoding utf8
}
Write-Card 'Type2Phaser' @('com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Triggers','com.wingedsheep.sdk.core.CounterType','com.wingedsheep.sdk.scripting.ModifyStats','com.wingedsheep.sdk.scripting.filters.unified.TargetFilter','com.wingedsheep.sdk.scripting.targets.TargetObject') @'
triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetObject(filter = TargetFilter.Creature.opponentControls(), id = "target"))
        effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 1, creature)
    }
    equipAbility("{3}")
    staticAbility { ability = ModifyStats(3, 0) }
'@
Write-Card 'VexingDrone' @('com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Triggers','com.wingedsheep.sdk.scripting.filters.unified.TargetFilter','com.wingedsheep.sdk.scripting.targets.TargetObject') @'
triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetObject(filter = TargetFilter.Creature.opponentControls(), id = "target"))
        effect = Effects.Tap(creature)
    }
'@
Write-Card 'Xenobotanist' @('com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Costs','com.wingedsheep.sdk.scripting.TimingRule') @'
activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
'@
Write-Card 'XindiSurveyors' @('com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Triggers','com.wingedsheep.sdk.core.CounterType','com.wingedsheep.sdk.scripting.GameObjectFilter','com.wingedsheep.sdk.scripting.targets.EffectTarget') @'
triggeredAbility {
        trigger = Triggers.controller.entersBattlefield(GameObjectFilter.Land)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
'@
Write-Card 'UnexploredTerritory' @('com.wingedsheep.sdk.dsl.Effects','com.wingedsheep.sdk.dsl.Costs','com.wingedsheep.sdk.dsl.Patterns','com.wingedsheep.sdk.scripting.TimingRule','com.wingedsheep.sdk.scripting.GameObjectFilter','com.wingedsheep.sdk.scripting.effects.SearchDestination') @'
activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }
'@
