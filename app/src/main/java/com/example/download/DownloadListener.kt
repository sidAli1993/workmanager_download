package com.example.download

interface DownloadListener {
        fun onDownloadFile(result: DownloadResult)
        fun onProgress(data: DownloadData, progress: Float) {}
}