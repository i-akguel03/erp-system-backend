$imageName = "erp-backend"
$containerName = "erp-backend-container"

Write-Host "1. Maven Build (ohne Tests)"
.\mvnw.cmd clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven Build fehlgeschlagen. Skript wird abgebrochen."
    exit 1
}

Write-Host "2. Docker Image bauen"
docker build -t $imageName .

if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Build fehlgeschlagen. Skript wird abgebrochen."
    exit 1
}

Write-Host "3. Alten Container stoppen und löschen (falls vorhanden)"
$oldContainer = docker ps -a --filter "name=$containerName" --format "{{.ID}}"

if ($oldContainer) {
    docker stop $containerName | Out-Null
    docker rm $containerName | Out-Null
    Write-Host "Alter Container $containerName wurde entfernt."
}

Write-Host "4. Neuen Container starten"
docker run -d -p 8080:8080 --name $containerName $imageName

if ($LASTEXITCODE -ne 0) {
    Write-Error "Container Start fehlgeschlagen."
    exit 1
}

Write-Host "Fertig! Container läuft auf Port 8080."
