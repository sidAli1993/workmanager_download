/*
package com.example.download

import android.content.Context
import android.util.Log
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList



class FilesDownloader(val context: Context) {

    companion object {
        private const val TAG = "DownloadFile"

        */
/** the maximum number of downloads that may be run in concurrency. *//*

        private const val MAX_CONCURRENCY = 100
        */
/** time in milliseconds between progress update for each item. *//*

        private const val PROGRESS_UPDATE_INTERVAL = 50L
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .build()

    private var callbacks: ArrayList<DownloadFileListener> = ArrayList()

    */
/**
     * active downloading files.
     *//*

    private val activeDownloads = ArrayList<DownloadData>()

    private var disposable: Disposable? = null
    private val publishSubject: PublishSubject<DownloadData> by lazy {
        val p = PublishSubject.create<DownloadData>()
        disposable = p.observeOn(Schedulers.io())
            .flatMap( {
                if (!activeDownloads.contains(it))
                    activeDownloads.add(it)

                Log.d(TAG, "Url: ${it.url}")

                var error: String?

                val file = File(it.path)

                try {

                    file.mkdirs()
                    if (file.exists())
                        file.delete()
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile()

                    val request = Request.Builder().url(it.url)

                    // add headers to request.

                    val response = client.newCall(request.build()).execute()
                    if (response.isSuccessful) {
                        error = null
                        val sink: BufferedSink = file.sink().buffer()
                        sink.writeAll(ProgressResponseBody(response.body!!) { bytesRead, contentLength, _ ->
                            callbacks.forEach { c ->
                                c.onProgress(it, bytesRead.toFloat() / contentLength.toFloat())
                            }
                        }.source())
                        sink.close()
                    }
                    else {
                        error = "$response"
                        Log.e(TAG, "Failed to download file: $response")
                    }

                }
                catch (e: Exception) {
                    error = e.message
                    e.printStackTrace()
                    Log.e(TAG, "Exception: " + e.message)
                }

                activeDownloads.remove(it)
                Observable.just(DownloadResult(it.identifier, it.path, error))
            }, true, MAX_CONCURRENCY)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                callbacks.forEach { c ->
                    c.onDownloadFile(it)
                }
            }
                , {
                    it.printStackTrace()
                })
        p
    }

    */
/**
     * pending new download, [addListener] to get
     * download callback.
     *//*

    @JvmOverloads fun download(path: String, url: String, identifier: Any = UUID.randomUUID().toString()) {
        val downloadData = DownloadData(identifier, url, path)
        if (!activeDownloads.contains(downloadData)) {
            activeDownloads.add(downloadData)
            publishSubject.onNext(downloadData)
        }
        else
            Log.d(TAG, "file already in pending!")
    }

    */
/**
     * add single download request and its callback,
     * note that this callback will also received in all [callbacks]
     * that have been registered via [addListener].
     *//*

    @JvmOverloads fun downloadWithCallback(url: String, path: String, callBack: (String?) -> Unit, onProgress: ((Float) -> Unit)? = null) {
        val id = UUID.randomUUID().toString()
        var downloadCallback: DownloadFileListener? = null
        downloadCallback = object : DownloadFileListener {
            override fun onDownloadFile(result: DownloadResult) {
                if (id == result.identifier) {
                    callBack.invoke(result.error)
                    callbacks.remove(downloadCallback!!)
                }
            }

            override fun onProgress(data: DownloadData, progress: Float) {
                if (id == data.identifier)
                    onProgress?.invoke(progress)
            }
        }
        callbacks.add(downloadCallback)
        download(path, url, id)
    }

    */
/**
     * callback to be called every time a file finish downloading.
     *
     * it will be invoked on the main thread.
     *//*

    fun addListener(callback: DownloadFileListener) = this.callbacks.add(callback)

    */
/**
     * remove the callback when you don't want it.
     *
     * you don't need to unregister if you call [dispose].
     *//*

    fun removeListener(callback: DownloadFileListener) = this.callbacks.remove(callback)

    fun dispose() {
        callbacks.clear()
        disposable?.dispose()
    }


    private class ProgressResponseBody constructor(private val responseBody: ResponseBody
                                                   , private val progressListener: (bytesRead: Long, contentLength: Long, done: Boolean) -> Unit) : ResponseBody() {

        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody.contentType()
        }

        override fun contentLength(): Long {
            return responseBody.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null)
                bufferedSource = source(responseBody.source()).buffer()
            return bufferedSource as BufferedSource
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {

                private var disposable: Disposable? = null

                private val updateHandler: PublishSubject<() -> Unit> by lazy {
                    val p = PublishSubject.create<() -> Unit>()
                    disposable = p.throttleLatest(PROGRESS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({call -> call.invoke() }
                            , {t -> t.printStackTrace() })
                    p
                }
                var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    if (bytesRead == -1L)
                        updateHandler.onComplete()
                    else
                        updateHandler.onNext { progressListener.invoke(totalBytesRead, responseBody.contentLength(), bytesRead == -1L) }

                    return bytesRead
                }

                override fun close() {
                    disposable?.dispose()
                    super.close()
                }
            }
        }

    }

}


interface DownloadFileListener {
    fun onDownloadFile(result: DownloadResult)
    fun onProgress(data: DownloadData, progress: Float) {}
}

data class DownloadData(val identifier: Any, val url: String, val path: String)

data class DownloadResult(val identifier: Any, val path: String, val error: String?) {
    val hasError = error != null
}*/
