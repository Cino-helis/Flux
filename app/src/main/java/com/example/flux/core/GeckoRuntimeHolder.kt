package com.example.flux.core

import org.mozilla.geckoview.GeckoRuntime

object GeckoRuntimeHolder {
    // Le ? indique que cette variable peut être null au démarrage
    var runtime: GeckoRuntime? = null 
}