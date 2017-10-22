# AVD-Gradle-Plugin
[![](https://jitpack.io/v/trevjonez/AVD-Gradle-Plugin.svg)](https://jitpack.io/#trevjonez/AVD-Gradle-Plugin)

Gradle plugin to assist in managing android virtual devices. 

## Usage

#### Installation
In the appropriate `build.gradle` file(s):

1. Add the jitpack maven repository and classpath dependency
```groovy
buildscript {
    repositories {
        maven { url "https://jitpack.io" }
    }
    dependencies {
        classpath "com.github.trevjonez:AVD-Gradle-Plugin:0.2.0"
    }
```

2. Apply the plugin
```groovy
apply plugin: 'AVD'
```

3. Configure your AVDs to create via the DSL options

```groovy
/* 
 * Simple configuration example 
 */
AVD {
    acceptAndroidSdkLicense true
    acceptAndroidSdkPreviewLicense true

    configs {
        'Nexus 5X API 26' {
            avd {
                abi 'x86'
                api 26
                type 'google_apis'
                deviceId 'Nexus 5X'
            }
        }
    }
}
```

```groovy
/* 
 * Full DSL Options 
 */
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
                coreCount 2 //optional defaults to # of cores on machine
                configIniProperty("key", "value) //add or overwrite config.ini file property
            }
            launchOption("-wipe-data")
            launchOption("-memory", "2048") //varargs
        }
    }
}
```

#### Tasks 
 - Install: `installSystemImage_${api-level}_${type}_${abi}` -> `installSystemImage_api26_GoogleApis_x86` 
 - Create: `createAvd_${escapedAvdName}` -> `createAvd_tablet_25`
 - Start: `startAvd_${escapedAvdName}` -> `startAvd_tablet_25`
 - Stop: `stopAvd_${escapedAvdName}` -> `stopAvd_tablet_25`
 
 ## Notes on Compatibility
 
 
 The plugin is developed against specific version of gradle and the android gradle plugin and sdk tools.
 In most cases using the latest version of gradle is safe but not guaranteed. 
 Currently this has only been tested against osx hosts and should work on any unix machine as well. 
 Highly likely that it won't work on windows. Open issues and lets talk if you need that. 
 
 AVD plugin version | Gradle version | Android plugin version
 ----- | ---- | -----
 0.2.0 | 4.2.1  | 3.0.0-rc2
 
 
## License
    Copyright 2016 Trevor Jones

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.