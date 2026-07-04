adb uninstall com.github.ma1co.pmcademo.app
del /q app\build\outputs\apk\release\*.apk
gradle assembleRelease
adb install -r app\build\outputs\apk\release\PMCADemo-release-0.7-6-g12f34e4-dirty.apk