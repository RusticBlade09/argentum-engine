$ErrorActionPreference = 'Stop'
$client = [System.Net.Http.HttpClient]::new()
$client.DefaultRequestHeaders.UserAgent.ParseAdd('ArgentumEngine/1.0')
$failed = @()
foreach ($path in Get-ChildItem "$PSScriptRoot/*.scryfall.json") {
    $c = Get-Content $path.FullName -Raw | ConvertFrom-Json
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Head, $c.image_uris.normal)
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne 200) { $failed += "$($c.name): $($response.StatusCode)" }
    $response.Dispose()
    $request.Dispose()
}
$client.Dispose()
if ($failed.Count) { throw ($failed -join "`n") }
Write-Output 'All 72 card image URLs returned HTTP 200.'
