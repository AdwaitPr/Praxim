sed -i '/\[versions\]/a \
paging = "3.3.4"\nsqlcipher = "4.5.4"\nwork = "2.10.0"' gradle/libs.versions.toml

sed -i '/\[libraries\]/a \
androidx-paging-runtime = { group = "androidx.paging", name = "paging-runtime", version.ref = "paging" }\nandroidx-paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }\nandroidx-room-paging = { group = "androidx.room", name = "room-paging", version.ref = "roomRuntime" }\nsqlcipher = { group = "net.zetetic", name = "android-database-sqlcipher", version.ref = "sqlcipher" }\nandroidx-sqlite-ktx = { group = "androidx.sqlite", name = "sqlite-ktx", version = "2.4.0" }\nandroidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }' gradle/libs.versions.toml
