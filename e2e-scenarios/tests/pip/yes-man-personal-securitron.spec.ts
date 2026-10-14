import { test, expect } from '../../fixtures/scenarioFixture'
import { PLAYER_BATTLEFIELD, cardByName } from '../../helpers/selectors'

test('Yes Man transfers control, draws on a reflexive trigger, and pays its owner in tapped Soldiers', async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Owner', player2Name: 'New Controller',
    player1: {
      hand: ['Lightning Bolt'],
      battlefield: [{ name: 'Yes Man, Personal Securitron' }, { name: 'Mountain' }],
      library: ['Plains', 'Plains', 'Plains', 'Plains'],
    },
    player2: { library: ['Mountain', 'Mountain', 'Mountain'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['PRECOMBAT_MAIN'],
  })
  const p1 = player1.gamePage
  const p2 = player2.gamePage
  await p1.clickCard('Yes Man, Personal Securitron')
  await p1.selectAction('Target opponent gains control')
  await p1.selectPlayer(player2.playerId)
  await p1.confirmTargets()
  await p1.screenshot('Yes Man activation targets an opponent')
  await p2.resolveStack('Target opponent gains control')
  await p2.expectOnBattlefield('Yes Man, Personal Securitron')
  await p1.expectHandSize(1)
  await p1.screenshot('Control transferred while the separate draw trigger awaits resolution')
  await p2.resolveStack('Draw two cards')
  await p1.expectHandSize(3)
  await p1.clickCard('Lightning Bolt')
  await p1.selectAction('Cast')
  await p1.selectTarget('Yes Man, Personal Securitron')
  await p1.confirmTargets()
  await p2.resolveStack('Lightning Bolt')
  await p2.resolveStack('Yes Man')
  await p1.expectOnBattlefield('Soldier')
  await p1.expectTapped('Soldier')
  await p2.expectNotOnBattlefield('Soldier')
  await expect(player1.page.locator(PLAYER_BATTLEFIELD).locator(cardByName('Soldier'))).toHaveCount(1)
  await p1.screenshot('Departed stolen Yes Man creates one tapped Soldier for its owner')
})
