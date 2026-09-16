with open('app/src/main/java/com/example/praxim/service/OverlayHUDService.kt', 'r') as f:
    content = f.read()

# Make sure imports are actually there
if 'import kotlinx.coroutines.Job' not in content:
    content = content.replace('import kotlinx.coroutines.flow.StateFlow', 'import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.Job')
if 'import java.util.concurrent.atomic.AtomicReference' not in content:
    content = content.replace('import kotlinx.coroutines.Job', 'import kotlinx.coroutines.Job\nimport java.util.concurrent.atomic.AtomicReference')

with open('app/src/main/java/com/example/praxim/service/OverlayHUDService.kt', 'w') as f:
    f.write(content)
