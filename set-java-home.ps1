# Set JAVA_HOME temporarily for this session
# Replace the path below with your actual JDK installation path

# Example paths (uncomment the one that matches your installation):
# $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_361"
# $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
# $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot"

# After setting the path above, run:
# .\mvnw.cmd spring-boot:run

Write-Host "Current JAVA_HOME: $env:JAVA_HOME"
Write-Host ""
Write-Host "To find your JDK installation, check these locations:"
Write-Host "  C:\Program Files\Java\"
Write-Host "  C:\Program Files (x86)\Java\"
Write-Host "  C:\Program Files\Eclipse Adoptium\"
Write-Host ""
Write-Host "After installing JDK, edit this file and uncomment the correct path."
