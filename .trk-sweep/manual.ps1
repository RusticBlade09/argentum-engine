$base = "$PSScriptRoot/../manual-scenarios/cards"
function Save-Scenario($folder, $slug, $hand, $board, $otherBoard, $lands, $stops) {
    $battlefield = @($board | ForEach-Object { @{ name = $_ } }) + @($lands | ForEach-Object { @{ name = $_ } })
    $scenario = [ordered]@{
        player1Name = 'TRK Tester'; player2Name = 'Opponent'
        player1 = @{ lifeTotal = 20; hand = @($hand); battlefield = $battlefield; graveyard = @(); library = @('Plains','Island','Mountain','Swamp','Forest','Plains','Island','Mountain','Swamp','Forest','Plains','Island') }
        player2 = @{ lifeTotal = 20; hand = @(); battlefield = @($otherBoard | ForEach-Object { @{ name = $_ } }); graveyard = @(); library = @('Plains','Island','Mountain','Swamp','Forest','Plains','Island','Mountain','Swamp','Forest','Plains','Island') }
        phase = 'PRECOMBAT_MAIN'; activePlayer = 1; priorityPlayer = 1
        player1StopAtSteps = @($stops); player2StopAtSteps = @(); player1OpponentStopAtSteps = @(); player2OpponentStopAtSteps = @()
    }
    New-Item -ItemType Directory -Force "$base/$folder" | Out-Null
    $scenario | ConvertTo-Json -Depth 20 | Set-Content "$base/$folder/$slug.json" -Encoding utf8
}
Save-Scenario 't' 'troubling-tribbles' @('Troubling Tribbles','Shock','Shock') @('Troubling Tribbles') @('Grizzly Bears') @('Plains','Plains','Plains','Mountain','Mountain') @('UPKEEP','END')
Save-Scenario 'l' 'lrell-fierce-visionary' @("L'Rell, Fierce Visionary") @('Grizzly Bears','Ornithopter') @('Grizzly Bears') @('Swamp','Swamp','Mountain','Mountain','Mountain') @('BEGIN_COMBAT','END')
Save-Scenario 'm' 'make-it-so' @('Make It So','Make It So') @('Grizzly Bears') @('Grizzly Bears') @('Island','Island','Island','Island') @()
Save-Scenario 'm' 'malfunctioning-holodeck' @('Malfunctioning Holodeck','Disenchant','Malfunctioning Holodeck') @() @('Grizzly Bears','Hill Giant') @('Plains','Plains','Plains','Plains','Plains','Plains') @()
