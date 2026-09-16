import re

with open('app/src/main/java/com/example/praxim/service/OverlayHUDService.kt', 'r') as f:
    content = f.read()

# Add imports if not present
if 'import kotlinx.coroutines.flow.StateFlow' not in content:
    content = re.sub(r'import kotlinx.coroutines.flow.MutableStateFlow',
                     'import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow',
                     content)
if 'import java.util.concurrent.atomic.AtomicReference' not in content:
    content = re.sub(r'import kotlinx.coroutines.Job',
                     'import kotlinx.coroutines.Job\nimport java.util.concurrent.atomic.AtomicReference',
                     content)

if 'private val _displayMode = MutableStateFlow<HudDisplayMode>(HudDisplayMode.Collapsed)' in content:
    content = content.replace(
        'private val _displayMode = MutableStateFlow<HudDisplayMode>(HudDisplayMode.Collapsed)',
        'private val _displayMode = MutableStateFlow<HudDisplayMode>(HudDisplayMode.Collapsed)\n    val displayMode: StateFlow<HudDisplayMode> = _displayMode'
    )

if 'private var scanJob: Job? = null' not in content:
    content = re.sub(
        r'private var screenCaptureManager: ScreenCaptureManager\? = null',
        'private var screenCaptureManager: ScreenCaptureManager? = null\n    private val frameJob = AtomicReference<Job?>(null)',
        content
    )

with open('app/src/main/java/com/example/praxim/service/OverlayHUDService.kt', 'w') as f:
    f.write(content)
