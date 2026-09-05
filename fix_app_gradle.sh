sed -i '/dependencies {/a \
  implementation(libs.androidx.paging.runtime)\n  implementation(libs.androidx.paging.compose)\n  implementation(libs.androidx.room.paging)\n  implementation(libs.sqlcipher)\n  implementation(libs.androidx.sqlite.ktx)\n  implementation(libs.androidx.work.runtime.ktx)' app/build.gradle.kts
