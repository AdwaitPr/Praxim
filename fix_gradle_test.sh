sed -i '/\[libraries\]/a \
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomRuntime" }' gradle/libs.versions.toml
