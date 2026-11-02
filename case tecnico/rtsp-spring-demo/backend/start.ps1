param([string]$FfmpegPath = $env:FFMPEG_PATH)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($FfmpegPath)) {
    $command = Get-Command ffmpeg -CommandType Application -ErrorAction SilentlyContinue
    if ($command) { $FfmpegPath = $command.Source }
}
if ([string]::IsNullOrWhiteSpace($FfmpegPath) -or -not (Test-Path -LiteralPath $FfmpegPath -PathType Leaf)) {
    throw 'FFmpeg nao encontrado. Instale-o e execute: .\start.ps1 -FfmpegPath C:\caminho\ffmpeg.exe'
}
$env:FFMPEG_PATH = (Resolve-Path -LiteralPath $FfmpegPath).Path
& $env:FFMPEG_PATH -version
if ($LASTEXITCODE -ne 0) { throw 'O executavel FFmpeg falhou na verificacao.' }
if ($env:JAVA_HOME -and -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    throw 'JAVA_HOME deve apontar para a raiz do JDK, sem o sufixo bin.'
}
Push-Location $PSScriptRoot
try {
    & mvn.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao executar o Spring. Consulte o log acima.' }
} finally { Pop-Location }