HESTIA Android final source-side fix

This package fixes the compile issues observed in the uploaded Android project:
- imported androidx.compose.foundation.verticalScroll
- changed Typography TextStyle construction to named parameters
- moved Modifier.weight() usage to Row scope for DetailStat and AdminStat

API base URL remains configured for the physical phone:
http://192.168.29.44:3000/

Build on Windows in Android Studio:
1. Open this android/ folder.
2. Verify local.properties points to your SDK.
3. Sync Gradle.
4. Build > Rebuild Project.
5. Run on the USB-connected Android phone.
