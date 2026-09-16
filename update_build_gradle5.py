import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Replace ksp with kapt or correct ksp syntax if needed, but the error is KSP-related
content = re.sub(r'ksp\("androidx\.room:room-compiler:2\.6\.1"\)', '"ksp"(libs.androidx.room.compiler)', content)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
