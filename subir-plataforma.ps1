param(
    [ValidateSet('Local', 'Compose')]
    [string]$Mode = 'Local',

    [switch]$Down,

    [switch]$Volumes,

    [switch]$SkipDocker,

    [switch]$SkipBuild,

    [int]$InitialDelaySeconds = 15
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Start-ServiceWindow {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title,

        [Parameter(Mandatory = $true)]
        [string]$Module,

        [hashtable]$Environment = @{}
    )

    $envAssignments = @()
    foreach ($entry in $Environment.GetEnumerator()) {
        $escapedValue = $entry.Value.Replace("'", "''")
        $envAssignments += "`$env:$($entry.Key)='$escapedValue'"
    }

    $commandParts = @(
        "Set-Location '$root'"
        $envAssignments
        ".\\mvnw.cmd -pl $Module spring-boot:run"
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    $command = $commandParts -join '; '

    Start-Process -FilePath "powershell.exe" `
        -ArgumentList @(
            '-NoExit',
            '-Command',
            "`$Host.UI.RawUI.WindowTitle = '$Title'; $command"
        ) `
        -WorkingDirectory $root
}

function Start-ComposeStack {
    param(
        [switch]$SkipBuild
    )

    $composeArgs = @('compose', 'up', '-d')
    if (-not $SkipBuild) {
        $composeArgs += '--build'
    }

    Write-Host "[EventMaster] Executando: docker $($composeArgs -join ' ')"
    & docker @composeArgs
}

function Stop-ComposeStack {
    param(
        [switch]$Volumes
    )

    $composeArgs = @('compose', 'down')
    if ($Volumes) {
        $composeArgs += '-v'
    }

    Write-Host "[EventMaster] Executando: docker $($composeArgs -join ' ')"
    & docker @composeArgs
}

Write-Host "[EventMaster] Diretório raiz: $root"
Write-Host "[EventMaster] Modo de execução: $Mode"

if (-not (Test-Path ".\\mvnw.cmd")) {
    throw "Arquivo mvnw.cmd não encontrado na raiz do monorepo."
}

if ($Down) {
    if ($SkipDocker) {
        Write-Host "[EventMaster] Aviso: -SkipDocker será ignorado quando -Down for utilizado."
    }
    if ($SkipBuild) {
        Write-Host "[EventMaster] Aviso: -SkipBuild será ignorado quando -Down for utilizado."
    }

    Write-Host "[1/1] Derrubando stack Docker Compose..."
    Stop-ComposeStack -Volumes:$Volumes

    Write-Host "[EventMaster] Stack Docker Compose encerrada com sucesso."
    Write-Host "[EventMaster] Exemplo: .\\subir-plataforma.ps1 -Down"
    Write-Host "[EventMaster] Exemplo removendo volumes: .\\subir-plataforma.ps1 -Down -Volumes"
    return
}

if ($Mode -eq 'Compose') {
    if ($SkipDocker) {
        throw "O parâmetro -SkipDocker não se aplica ao modo Compose. Use -Mode Local para reaproveitar apenas a infraestrutura existente."
    }

    Write-Host "[1/1] Subindo stack completa via Docker Compose..."
    Start-ComposeStack -SkipBuild:$SkipBuild

    Write-Host "[EventMaster] Stack Docker Compose disparada com sucesso."
    Write-Host "[EventMaster] Exemplo: .\\subir-plataforma.ps1 -Mode Compose"
    Write-Host "[EventMaster] Exemplo sem rebuild: .\\subir-plataforma.ps1 -Mode Compose -SkipBuild"
    return
}

if (-not $SkipDocker) {
    Write-Host "[1/5] Subindo infraestrutura com Docker Compose..."
    docker compose up -d
}
else {
    Write-Host "[1/5] Infraestrutura ignorada por parâmetro (-SkipDocker)."
}

Write-Host "[2/5] Subindo user-service..."
Start-ServiceWindow -Title "EventMaster - user-service" -Module "services/user-service" -Environment @{
    DB_PORT = '3307'
}
Start-Sleep -Seconds $InitialDelaySeconds

Write-Host "[3/5] Subindo event-service e ticket-service..."
Start-ServiceWindow -Title "EventMaster - event-service" -Module "services/event-service" -Environment @{
    DB_PORT = '3307'
    KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
}
Start-ServiceWindow -Title "EventMaster - ticket-service" -Module "services/ticket-service" -Environment @{
    DB_PORT = '3307'
    KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
    REDIS_HOST = 'localhost'
    REDIS_PORT = '6379'
}
Start-Sleep -Seconds $InitialDelaySeconds

Write-Host "[4/5] Subindo order-service e payment-service..."
Start-ServiceWindow -Title "EventMaster - order-service" -Module "services/order-service" -Environment @{
    DB_PORT = '3307'
    KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
}
Start-ServiceWindow -Title "EventMaster - payment-service" -Module "services/payment-service" -Environment @{
    KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
}
Start-Sleep -Seconds $InitialDelaySeconds

Write-Host "[5/5] Subindo gateway-service..."
Start-ServiceWindow -Title "EventMaster - gateway-service" -Module "services/gateway-service"

Write-Host "[EventMaster] Comando disparado com sucesso."
Write-Host "[EventMaster] Use -SkipDocker se a infraestrutura já estiver rodando."
Write-Host "[EventMaster] Exemplo: .\\subir-plataforma.ps1 -SkipDocker"

