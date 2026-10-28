& "$PSScriptRoot/briefs.ps1"
$scripts = @{}
$scripts.EmergencyMedicalHologram = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then Effects.GainLife(1)
    }
'@
$scripts.FederationProbe = @'
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +1/+1 until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(1, 1, creature)
            },
            Mode(effect = Effects.Surveil(1), description = "Surveil 1.")
        )
    }
'@
$scripts.FreeBorgRevolutionaries = @'
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }
'@
$scripts.GeneralChangColdWarrior = @'
    activatedAbility {
        cost = Costs.Mana("{1}") + Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Klingon").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }
'@
$scripts.HesDeadJim = @'
    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.Surveil(1)
    }
'@
$scripts.HitIt = @'
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl)
    }
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmount.Fixed(5),
            filter = GameObjectFilter.Land,
            prompt = "Put a land card from among them onto the battlefield tapped",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve
        )
    }
'@
$scripts.Horta = @'
    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
'@
$scripts.HumpbackWhales = @'
    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))
'@
$scripts.InterstellarEffigy = @'
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
    }
'@
$scripts.LRellFierceVisionary = @'
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"),
            keywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END
        )
    }
    activatedAbility {
        cost = Costs.Mana("{2}") + Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        effect = Effects.Surveil(1)
    }
'@
$scripts.MakeItSo = @'
    spell {
        effect = ModalEffect.chooseOne(
            mode("Untap target permanent you control. Target player draws two cards.") {
                val permanent = target(TargetFilter(GameObjectFilter.Permanent.youControl()))
                val player = target(Targets.Player)
                effect = Effects.Untap(permanent) then Effects.DrawCards(2, player)
            },
            Mode(
                effect = Effects.CreateToken(
                    power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Officer")
                ) then Effects.DrawCards(2),
                description = "Create a 1/1 red Officer creature token and draw two cards."
            )
        )
    }
'@
$scripts.MalfunctioningHolodeck = @'
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))
    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ExileUntilLeaves(permanent)
    }
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }
'@
$scripts.Moopsy = @'
    keywords(Keyword.DEATHTOUCH)
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.GrantKeyword(Keyword.REACH, EffectTarget.Self)
    }
'@
$scripts.Mugato = @'
    keywords(Keyword.REACH, Keyword.VIGILANCE, Keyword.DEATHTOUCH)
'@
$scripts.MunitionsEnthusiast = @'
    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.self.dies()
        val damageTarget = target(Targets.Any)
        effect = Effects.DealDamage(1, damageTarget)
    }
'@
$scripts.OrganicAvulsionUnit = @'
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ForEachInGroup(
            GroupFilter((GameObjectFilter.Artifact and GameObjectFilter.Creature).youControl()),
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.IterationEntity)
        )
    }
'@
$imports = @'
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
'@
foreach ($slug in $scripts.Keys) {
    $skeleton = Get-Content "$PSScriptRoot/$slug.skeleton.kt" -Raw
    $body = $scripts[$slug]
    $needed = @($imports -split "`n" | Where-Object { $class = ($_ -split '\.')[-1].Trim(); $body -match "\b$class\b" }) -join "`n"
    $output = $skeleton.Replace('import com.wingedsheep.sdk.dsl.card', "$needed`nimport com.wingedsheep.sdk.dsl.card").Replace('    // ABILITIES GO HERE', $body.TrimEnd())
    $output | Set-Content "$PSScriptRoot/../mtg-sets/2026/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/trk/cards/$slug.kt" -Encoding utf8
}
