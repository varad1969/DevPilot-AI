$env:OPENAI_API_KEY = "${env:OPENAI_API_KEY}"
$env:GITHUB_CLIENT_ID = "${env:GITHUB_CLIENT_ID}"
$env:GITHUB_CLIENT_SECRET = "${env:GITHUB_CLIENT_SECRET}"
$env:APP_FRONTEND_URL = "http://localhost:3000"
$env:APP_ENCRYPTION_PASSWORD = "devpilot_secret_master_key_12345"
$env:DB_URL = "jdbc:postgresql://localhost:5433/devpilot"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"

& .\mvnw.cmd spring-boot:run