package com.example.download

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

class Downloader {


    private val client: OkHttpClient = OkHttpClient.Builder()
        .build()

    private var callbacks: ArrayList<DownloadListener> = ArrayList()

    private var disposable: Disposable? = null

    companion object {
        /** time in milliseconds between progress update for each item. */
        private const val PROGRESS_UPDATE_INTERVAL = 100L

        /** the maximum number of downloads that may be run in concurrency. */
        private const val MAX_CONCURRENCY = 100
    }


    private val publishSubject: PublishSubject<DownloadData> by lazy {
        val publisher = PublishSubject.create<DownloadData>()
        disposable = publisher.observeOn(Schedulers.io())
            .flatMap({ downloadData ->
                var error: String?


                try {
                    val file = File(downloadData.path)
                    file.mkdirs()
                    if (file.exists())
                        file.delete()
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile()

                    val request = Request.Builder().url(downloadData.url)
                    val response = client.newCall(request.build()).execute()
                    if (response.isSuccessful) {
                        error = null
                        val sink: BufferedSink = file.sink().buffer()
                        sink.writeAll(ProgressResponseBody(response.body!!) { bytesRead, contentLength, isDone ->
                            callbacks.forEach { downloadListener ->
                                downloadListener.onProgress(
                                    downloadData,
                                    bytesRead.toFloat() / contentLength.toFloat()
                                )
                            }
                        }.source())
                        sink.close()
                    } else {
                        error = "$response"
                        Log.e("DownloadFail", "Failed to download file: $response")
                    }

                } catch (e: Exception) {
                    error = e.message
                    e.printStackTrace()
                    Log.e("DownloadFailException", "Exception: " + e.message)
                }


                Observable.just(DownloadResult(downloadData.identifier, downloadData.path, error))
            }, true, MAX_CONCURRENCY)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({downloadResult->
                callbacks.forEach { downloadListener ->
                    downloadListener.onDownloadFile(downloadResult)
                }
            }, {
                it.printStackTrace()
            })
        publisher
    }


    private class ProgressResponseBody constructor(
        private val responseBody: ResponseBody,
        private val progressListener: (bytesRead: Long, contentLength: Long, done: Boolean) -> Unit
    ) : ResponseBody() {

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
                    val pSubject = PublishSubject.create<() -> Unit>()
                    disposable = pSubject.throttleLatest(
                        PROGRESS_UPDATE_INTERVAL,
                        TimeUnit.MILLISECONDS,
                        true
                    )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ call -> call.invoke() }, { t -> t.printStackTrace() })
                    pSubject
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
                        updateHandler.onNext {
                            progressListener.invoke(
                                totalBytesRead,
                                responseBody.contentLength(),
                                bytesRead == -1L
                            )
                        }

                    return bytesRead
                }

                override fun close() {
                    disposable?.dispose()
                    super.close()
                }
            }
        }

    }


    /**
     * pending new download, [addListener] to get
     * download callback.
     */
    @JvmOverloads
    fun download(path: String, url: String, identifier: Any = UUID.randomUUID().toString()) {
        val downloadData = DownloadData(identifier, url, path)
        publishSubject.onNext(downloadData)
    }

    fun addListener(callback: DownloadListener) = this.callbacks.add(callback)


}