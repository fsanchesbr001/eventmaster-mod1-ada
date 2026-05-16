param(
    [switch]$SkipDocker,
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

Write-Host "[EventMaster] Diretório raiz: $root"

if (-not (Test-Path ".\\mvnw.cmd")) {
    throw "Arquivo mvnw.cmd não encontrado na raiz do monorepo."
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

