package net.gini.gradle

object Versions {

    object Tools {
        const val kotlin = "1.5.30"
        const val dokka = "1.5.0"
    }

    object Android {
        const val compileSdk = 31
        const val minSdk = 21
        const val targetSdk = 31
    }

    object Deps {
        const val androidXCore = "1.6.0"
        const val androidXMultiDex = "2.0.1"
        const val moshi = "1.12.0"
        const val coroutines = "1.5.2"
    }

    object Test {
        const val mockito = "3.10.0"
        const val androidXTest = "1.4.0"
        const val androidXTestJUnit = "1.1.3"
    }
}