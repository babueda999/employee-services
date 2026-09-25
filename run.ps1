# Runs the packaged jar with the JVM trust store needed for outbound
# HTTPS calls (e.g. to OpenAI) in this environment. See .claude/CLAUDE.md
# for the known PKIX/SSL trust-chain issue this works around.
$jar = Get-ChildItem -Path "$PSScriptRoot\target" -Filter "employee-*.jar" |
    Where-Object { $_.Name -notmatch '\.original$' } |
    Select-Object -First 1

if (-not $jar) {
    Write-Error "No jar found in target/. Run '.\mvnw.cmd clean package' first."
    exit 1
}

java "-Djavax.net.ssl.trustStore=$PSScriptRoot\.certs\cacerts" -jar $jar.FullName
