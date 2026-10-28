$ErrorActionPreference = 'Stop'
function Write-Card($name, $imports, $script) {
    $body = Get-Content "$PSScriptRoot/$name.skeleton.kt" -Raw
    $body = $body.Replace('import com.wingedsheep.sdk.dsl.card', "import com.wingedsheep.sdk.dsl.card`n$imports")
    $body = $body.Replace('    // ABILITIES GO HERE', $script)
    [IO.File]::WriteAllText("$PSScriptRoot/../mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards/$name.kt", $body, [Text.UTF8Encoding]::new($false))
}
Write-Card 'PlanetaryPatrol' @'
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
'@ @'
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }
'@
Write-Card 'PrimaryUnimatrixAdjunct' @'
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
'@ @'
    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }
'@
Write-Card 'RelentlessDrednok' @'
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.then
import com.wingedsheep.sdk.scripting.targets.EffectTarget
'@ @'
    keywords(Keyword.LIFELINK)
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }
'@
Write-Card 'RogueArtificialIntelligence' @'
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.Mode
'@ @'
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                Mode.noTarget(Effects.GainLife(3), "You gain 3 life"),
                Mode.noTarget(Effects.Surveil(2), "Surveil 2")
            )
        )
    }
'@
Write-Card 'RutherfordUpgradedEnsign' @'
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
'@ @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 6,
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card from among them and put it into your hand"
        )
    }
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype("Spacecraft"))
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }
'@
Write-Card 'SilicateSurveyor' @'
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
'@ @'
    keywords(Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Exile.impulse(1, MayPlayExpiry.UntilEndOfNextTurn)
    }
'@
Write-Card 'SyndicateLiquidators' @'
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
'@ @'
    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }
'@
Write-Card 'TeaEarlGreyHot' @'
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
'@ @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Modal(
            modes = listOf(
                mode("Deal 2 damage to target opponent") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.DealDamage(2, opponent)
                },
                mode("You gain 3 life") {
                    effect = Effects.GainLife(3)
                }
            )
        )
    }
'@
Write-Card 'TroublingTribbles' @'
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.CantBlock
'@ @'
    staticAbility {
        ability = CantBlock()
    }
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateTokenCopyOfSelf()
    }
'@
