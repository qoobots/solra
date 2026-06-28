param(
    [string]$RootPath = "d:\05workspaces\solra"
)

$emptyDirs = Get-ChildItem -Path $RootPath -Recurse -Directory | Where-Object {
    (Get-ChildItem $_.FullName -File).Count -eq 0
}

foreach ($dir in $emptyDirs) {
    $gitkeepPath = Join-Path $dir.FullName ".gitkeep"
    if (-not (Test-Path $gitkeepPath)) {
        New-Item -ItemType File -Path $gitkeepPath -Force | Out-Null
        $relativePath = $dir.FullName.Replace($RootPath, "").TrimStart("\")
        Write-Host "Created .gitkeep: $relativePath"
    }
}

Write-Host "`nTotal .gitkeep files created: $($emptyDirs.Count)" -ForegroundColor Green
