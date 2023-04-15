package com.example.download

data class DownloadResult(val identifier: Any, val path: String, val error: String?) {
    val hasError = error != null
}