package com.orderfast

import com.easygoband.toolkit.sdk.bundle.components.Toolkit

/**
 * Singleton para mantener la referencia al Toolkit
 * Permite acceder al toolkit desde el Worker en segundo plano
 */
object ToolkitProvider {
    private var toolkit: Toolkit? = null

    fun setToolkit(instance: Toolkit) {
        toolkit = instance
    }

    fun getToolkit(): Toolkit? = toolkit

    fun clearToolkit() {
        toolkit = null
    }
}