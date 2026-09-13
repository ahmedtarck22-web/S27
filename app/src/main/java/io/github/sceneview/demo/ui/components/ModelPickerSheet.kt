package io.github.sceneview.demo.ui.components

/**
 * Represents a 3D model object to display in Object, AR, or MR mode.
 * All extraneous preset models have been removed as requested.
 * Users open files directly from their device storage.
 */
sealed class SceneObject(
    val title: String,
    val assetPath: String? = null,
    val localPath: String? = null,
    val defaultScale: Float = 1.0f
) {
    data object DefaultModel : SceneObject(
        title = "3D Model",
        assetPath = "models/damaged_helmet.glb",
        defaultScale = 1.0f
    )

    data class CustomFile(val fileName: String, val path: String) : SceneObject(
        title = fileName,
        localPath = path,
        defaultScale = 1.0f
    )

    val isCustom: Boolean get() = this is CustomFile
}
