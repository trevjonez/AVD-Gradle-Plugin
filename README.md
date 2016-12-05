# AVD-Gradle-Plugin
Gradle plugin to assist in managing android virtual devices

`android list sdk -a -e`

```groovy
AVD {
    sys-img-xml //optional default "https://dl.google.com/android/repository/sys-img/android/sys-img.xml"
    configs {
        tablet_25 {
            abi x86_64
            api 25 
            sdPath relative/path //optional
            sdSize 1000M //optional
            skin WXGA
            type google_apis //optional default "default"
            port 12345 //optional auto assign default
            launch_options ""
            wipe_data false //(default true)
            use_data relative/path //optional
        }
    }
}
```

tasks
 - (root task) download sys-img list `https://dl.google.com/android/repository/sys-img/android/sys-img.xml`
    - `android update sdk --no-ui --all --filter "sys-img-x86_64-google_apis-25"`
 
 - (per unique sys image) install/update sys-img `if (!file("$ANDROID_HOME/system_images/android-$api/$type/$abi/source.properties").exists())` 
    - This file has the rev number to match against the sys-img.xml file.
 
 - (per defined config) start emulator //figure out how to check if already running and skip?
 
 - (per defined config) stop emulator //only if it is running?