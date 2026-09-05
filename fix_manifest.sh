sed -i 's/android:allowBackup="true"/android:allowBackup="false"/' app/src/main/AndroidManifest.xml
sed -i 's/android:fullBackupContent="@xml\/backup_rules"/android:fullBackupContent="false"/' app/src/main/AndroidManifest.xml
