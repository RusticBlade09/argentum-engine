$path = "$PSScriptRoot/../backlog/sets/star-trek/cards.md"
$text = Get-Content $path -Raw
$ready = Get-Content "$PSScriptRoot/names.json" -Raw | ConvertFrom-Json
foreach ($name in $ready) { $text = $text.Replace("- [ ] $name`r`n", "- [x] $name`r`n").Replace("- [ ] $name`n", "- [x] $name`n") }
$cache = Get-Content 'C:/Users/vlad2/.cache/scryfall/set-trk.json' -Raw | ConvertFrom-Json
$checked = @{}
foreach ($m in [regex]::Matches($text, '(?m)^- \[([x ])\] (.+)\r?$')) { $checked[$m.Groups[2].Value.TrimEnd("`r")] = ($m.Groups[1].Value -eq 'x') }
$names = @($cache.extra_names | Sort-Object)
$done = @($names | Where-Object { $checked[$_] }).Count
$entries = $names | ForEach-Object { if ($checked[$_]) { "- [x] $_" } else { "- [ ] $_" } }
$header = $text.Substring(0, $text.IndexOf('- [', $text.IndexOf('### Extras')))
$header = $header -replace '\*\*Set Size:\*\* \d+ cards', "**Set Size:** $($names.Count) cards"
$header = $header -replace '\*\*Implemented:\*\* \d+ / \d+', "**Implemented:** $done / $($names.Count)"
$header = $header -replace '\| Extras\s*\|\s*\d+\s*\|\s*\d+\s*\|', "| Extras     | $($names.Count)   | $done   |"
$header = $header -replace 'Preview snapshot from October 26, 2026', 'Preview snapshot from October 28, 2026'
($header + ($entries -join "`n") + "`n") | Set-Content $path -Encoding utf8
