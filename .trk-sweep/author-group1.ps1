$ErrorActionPreference = 'Stop'
$scripts = @{}
$scripts.AssaultDrone = @'
    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }
'@
$scripts.AssemblySpecialist = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(1, 1, emptySet(), setOf("Borg"), artifactToken = true)
    }
'@
$scripts.Batleth = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetObject(filter = TargetFilter.Creature.opponentControls(), id = "target"))
        effect = Effects.ModifyStats(-2, -2, creature)
    }
    equipAbility("{2}")
    staticAbility { ability = ModifyStats(2, 0) }
'@
$scripts.BattleScarredSurvivalist = @'
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        oncePerTurn = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
'@
$scripts.CaptainsTutelage = @'
    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature, id = "target")
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.EnchantedCreature)
    }
    staticAbility { ability = ModifyStats(2, 0) }
'@
$scripts.CetaceanOps = @'
    keywordAbility(KeywordAbility.ward("{1}"))
    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
'@
$scripts.ChaDIchInvestigator = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2, toughness = 1, colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"), keywords = setOf(Keyword.HASTE)
        )
    }
'@
$scripts.CloisteredTelepath = @'
    keywords(Keyword.FLASH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1) then Effects.Discard(1)
    }
'@
$scripts.ColdBloodedCrew = @'
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetObject(filter = TargetFilter.Creature, id = "target"))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }
    staticAbility { ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(2)) }
'@
$scripts.CollectiveDrone = @'
    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactCreature).count())
'@
$scripts.CommandDecision = @'
    spell {
        effect = Effects.Modal(listOf(
            mode("Destroy target creature with power 4 or greater") {
                val creature = target(TargetObject(filter = TargetFilter.Creature.powerAtLeast(4), id = "target"))
                effect = Effects.Destroy(creature)
            },
            mode("Draw a card") { effect = Effects.DrawCards(1) }
        ))
    }
'@
$scripts.CommandSubadjunct = @'
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }
'@
$scripts.ConstableOdo = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(listOf(
            mode("Tap an opponent's creature and put two stun counters on it") {
                val creature = target(TargetObject(filter = TargetFilter.Creature.opponentControls(), id = "target"))
                effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 2, creature)
            },
            mode("Investigate twice") { effect = Effects.Investigate(2) }
        ))
    }
'@
$scripts.CyberneticSpecialist = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Surveil(1)
    }
    activatedAbility {
        cost = Costs.Mana("{1}") + Costs.Tap
        val card = target(TargetObject(filter = TargetFilter.CardInGraveyard.ownedByYou(), id = "target"))
        effect = Effects.PutOnBottomOfLibrary(card)
    }
'@
$scripts.DVanaTendiOptimisticEnsign = @'
    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetObject(filter = TargetFilter.Permanent.other(), id = "target"))
        effect = Effects.Untap(permanent)
    }
    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Surveil(1)
    }
'@
$scripts.DataNewLife = @'
    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
'@
$imports = @'
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
'@
$dest = 'mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards'
foreach ($name in $scripts.Keys) {
    $content = Get-Content "$PSScriptRoot/$name.skeleton.kt" -Raw
    $content = $content.Replace('    // ABILITIES GO HERE', $scripts[$name])
    $usedImports = @($imports -split "`r?`n" | Where-Object { $type = ($_ -split '\.')[-1]; $content -match "\b$type\b" }) -join "`n"
    $content = $content.Replace('import com.wingedsheep.sdk.dsl.card', "$usedImports`nimport com.wingedsheep.sdk.dsl.card")
    [System.IO.File]::WriteAllText("$dest/$name.kt", $content, [System.Text.UTF8Encoding]::new($false))
}
