$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

if (-not (Get-Command javac.exe -ErrorAction SilentlyContinue)) {
    throw "JDK 17 or later is required. Install a JDK and make sure javac is on PATH."
}
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw "Java is not on PATH. Install a JDK 17 or later."
}

$libraryDirectory = Join-Path $PSScriptRoot "lib"
$outputDirectory = Join-Path $PSScriptRoot "out"
$libraries = @(Get-ChildItem -LiteralPath $libraryDirectory -Filter "*.jar" -File)
if ($libraries.Count -eq 0) {
    throw "Dependency JARs are missing. Restore the JAR files in the lib folder."
}

if (-not $env:POS_DB_USER) {
    $env:POS_DB_USER = "root"
}
if (-not $env:POS_DB_URL) {
    $env:POS_DB_URL = "jdbc:mysql://localhost:3306/computer_accessories_pos?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true"
}
if (-not $env:POS_DB_PASSWORD) {
    $securePassword = Read-Host "Enter the MySQL password for $env:POS_DB_USER" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:POS_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

$sourceFiles = @(Get-ChildItem -Path (Join-Path $PSScriptRoot "src\main\java") -Filter "*.java" -File -Recurse)
if ($sourceFiles.Count -eq 0) {
   throw "No Java source files found under src\main\java."
}
if (Test-Path -LiteralPath $outputDirectory) {
   Remove-Item -LiteralPath $outputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$dependencyPath = Join-Path $libraryDirectory "*"
$sourcePaths = @($sourceFiles | ForEach-Object { $_.FullName })
& javac.exe --release 17 -encoding UTF-8 -cp $dependencyPath -d $outputDirectory $sourcePaths
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed with exit code $LASTEXITCODE."
}

$resourceDirectory = Join-Path $PSScriptRoot "src\main\resources"
if (Test-Path -LiteralPath $resourceDirectory) {
    Copy-Item -Path (Join-Path $resourceDirectory "*") -Destination $outputDirectory -Recurse -Force
}

$runtimePath = "$outputDirectory;$dependencyPath"
& java.exe -cp $runtimePath com.computeraccessories.pos.App
exit $LASTEXITCODE
