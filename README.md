# AVD-Gradle-Plugin
Gradle plugin to assist in managing android virtual devices

```groovy
AVD {
    autoUpdate false //default true (optional)
    acceptAndroidSdkLicense true //default false (required)
    acceptAndroidSdkPreviewLicense true //default false (required)
    configs {
        tablet_25 {
            avd {
                abi "x86" | "x86_64" | "armeabi-v7a" | "arm64-v8a" //(default x86_64)
                api 25 //(default highest stable api)
                type 'google_apis_playstore' | 'google_apis' | 'default' | 'android-wear' | 'android-tv' //(default google_apis)
                deviceId "pixel" //from avdmanager list device
                sdSize "1000M" //optional
    
            }
            emu {
                skin "nexus_5x"
                port 12345 //optional auto assign default
                launch_options ""
                wipe_data false //(default true)
                use_data file('path') //optional
            }
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

 `avdmanager create avd --name '26_6P_Playstore' --package 'system-images;android-26;google_apis_playstore;x86' --device 'Nexus 6P' --tag 'google_apis_playstore'`