sed -i 's/db.scanAuditDao().insert(entity)/runBlocking { db.scanAuditDao().insert(entity) }/' app/src/main/java/com/praxim/core/portability/EncryptedArchiveManager.kt
sed -i 's/import kotlinx.coroutines.withContext/import kotlinx.coroutines.withContext\nimport kotlinx.coroutines.runBlocking/' app/src/main/java/com/praxim/core/portability/EncryptedArchiveManager.kt
