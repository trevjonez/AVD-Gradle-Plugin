# AVD-Gradle-Plugin
Gradle plugin to assist in managing android virtual devices

```groovy
AVD {
    //By default the install task(s) up to date check will run `sdkmanager` to see if an update is available
    //If disabled this will only check if the system image dir is present
    autoUpdate false //default true (optional)
    
    acceptAndroidSdkLicense true //default false (required)
    acceptAndroidSdkPreviewLicense true //default false (required)
    
    //Note: if any proxy settings are specified all must be present
    proxyType "http" //default null (optional)
    proxyHost "localhost" //default null (optional)
    proxyPort 8080 //default null (optional)
    
    noHttps true //default false (optional)
    
    configs {
        tablet_25 {
            avd {
                abi "x86" | "x86_64" | "armeabi-v7a" | "arm64-v8a" //(default x86_64)
                api 25 //(default highest stable api)
                type 'google_apis_playstore' | 'google_apis' | 'default' | 'android-wear' | 'android-tv' //(default google_apis)
                deviceId "pixel" //from `avdmanager list device`
                sdSize "1000M" //optional
            }
            launch_option("-wipe-data")
            launch_option("-memory", "2048")
        }
    }
}
```

## Tasks 
 - Install: `installSystemImage_${api-level}_${type}_${abi}` -> `installSystemImage_api26_GoogleApis_x86` 
 - (per avd) create avd TODO()
 - (per avd) start avd TODO()
 - (per avd) stop avd TODO()
 
 ## Usage
 Don't. It isn't usable yet. 
 
 ## Scratch pad

 `avdmanager create avd --name '26_6P_Playstore' --package 'system-images;android-26;google_apis_playstore;x86' --device 'Nexus 6P'`