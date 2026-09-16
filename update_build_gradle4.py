import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Add externalNativeBuild
if 'externalNativeBuild {' not in content:
    replacement = """
  buildFeatures {
    compose = true
    buildConfig = true
  }
  externalNativeBuild {
    cmake {
      path = file("../engine-praxim/core-capture/CMakeLists.txt")
      version = "3.22.1"
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
"""
    content = re.sub(r'  buildFeatures \{\s*compose = true\s*buildConfig = true\s*\}\s*testOptions \{ unitTests \{ isIncludeAndroidResources = true \} \}', replacement, content)

# Remove dependencies
deps_to_remove = [
    r'\n\s*implementation\(platform\(libs\.firebase\.bom\)\)',
    r'\n\s*implementation\(libs\.converter\.moshi\)',
    r'\n\s*implementation\(libs\.firebase\.ai\)',
    r'\n\s*implementation\(libs\.firebase\.appcheck\.recaptcha\)',
    r'\n\s*implementation\(libs\.logging\.interceptor\)',
    r'\n\s*implementation\(libs\.moshi\.kotlin\)',
    r'\n\s*implementation\(libs\.okhttp\)',
    r'\n\s*implementation\(libs\.retrofit\)',
    r'\n\s*"ksp"\(libs\.moshi\.kotlin\.codegen\)',
]

for dep in deps_to_remove:
    content = re.sub(dep, '', content)

# Add explicit dependencies if missing
deps_to_add = [
    'implementation("com.google.mlkit:text-recognition:16.0.1")',
    'implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")',
    'implementation("com.google.mediapipe:tasks-genai:0.10.14")',
]

for dep in deps_to_add:
    if dep not in content:
        content = re.sub(r'dependencies \{', 'dependencies {\n  ' + dep, content)


with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
