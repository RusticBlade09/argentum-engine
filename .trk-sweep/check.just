set working-directory := '..'

scenarios:
    scripts/gradle-locked :mtg-sets:2026:tests:test --tests '*TroublingTribblesScenarioTest' --tests '*LRellFierceVisionaryScenarioTest' --tests '*MakeItSoScenarioTest' --tests '*MalfunctioningHolodeckScenarioTest'
