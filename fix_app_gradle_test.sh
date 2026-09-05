sed -i '/dependencies {/a \
  androidTestImplementation(libs.androidx.room.testing)' app/build.gradle.kts
