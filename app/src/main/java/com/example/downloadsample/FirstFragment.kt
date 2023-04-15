package com.example.downloadsample

import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.download.*
import com.example.downloadsample.databinding.FragmentFirstBinding
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var dataArray: ArrayList<DataArray> = ArrayList()

    data class DataArray(val id: Int, val url: String, val path: String)
    val url = "https://testexifirmware.blob.core.windows.net/53ec8bfc-92e0-4434-a447-4199f9fb11bc/M000-TESTOS-V9-7.tar.gz"

    var counter = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        for (counter in 0..3){

           val filePath =  File(
                context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "$counter.tar"
            ).absolutePath


            dataArray.add(DataArray(counter,url,filePath))
        }

        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

//            callDownload()

          val filePath =   File(
                context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "myFiles"+ ".tar"
            ).absolutePath

//            val downloadLink = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            val downloadLink = "https://testexifirmware.blob.core.windows.net/53ec8bfc-92e0-4434-a447-4199f9fb11bc/M000-TESTOS-V9-7.tar.gz"



            if(dataArray.size>0){
                downloadFile(
                    filePath = dataArray[counter].path,
                    downloadLink = dataArray[counter].url

                )
            }


        }
    }




    private fun downloadFile(filePath:String,downloadLink:String){
        val downloader =Downloader()
        downloader.download(
            path = filePath,
            url = downloadLink,
            identifier = "1"

        )

        downloader.addListener(object : DownloadListener {

            override fun onDownloadFile(result: DownloadResult){
                if (result.hasError) {
                    Log.e("FileStatus","downloadingError->"+ result.error!!)

                    /*ResponseDownload.Error(
                        result.error ?: "download simple error"
                    )*/

                } else {
                    Log.e("FileStatus","downloadingSuccess->")
                    counter++
                    if(counter<dataArray.size){
                        downloadFile(
                            filePath = dataArray[counter].path,
                            downloadLink = dataArray[counter].url

                        )
                    }
                    // all files loaded
                    val filePath =  File(
                        context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "$counter.tar"
                    ).absolutePath

                    val filesFolder = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val files: Array<File> = filesFolder?.listFiles() as Array<File>
                    for (file in files.indices) {
                        Log.d("Files", "FileName:" + files[file].name)

//                        convertFileToBase64(files[counter])
                    }

                }
            }

            override fun onProgress(data: DownloadData, progress: Float) {
//                Log.e("in progress", "$progress")

                // convert in double then it will work
                val progressInPercent :Int = (progress * 100).toInt()


                Log.e("FileStatus", "downloaded percentage-> $progressInPercent out of total= 100%")

                /*ResponseDownload.OnDownload(
                    "100",
                    "$progressInPercent"
                )*/

            }


        })

    }

    /*private fun callDownload(){
       val file = File(
            context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "myFile"+ ".pdf"
        )
        val filepath = file.absolutePath

        val filesDownloader = FilesDownloader(requireContext())

        val testLink = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
//        val bookLink = "https://www.taleem360.com/download/entry-test-preparation-book-for-national-mdcat-latest-jti"


        filesDownloader.download(
            filepath,
            testLink,
            "1"

        )



        filesDownloader.addListener(object : DownloadFileListener {
            override fun onDownloadFile(result: DownloadResult) {
                if (result.hasError) {
                    Log.e("FileStatus","downlodingError->"+ result.error!!)

                        ResponseDownload.Error(
                            result.error ?: "download imple error"
                        )

                } else {
                    Log.e("FileStatus","downlodingSuccess->")

                    *//*if (result.identifier == chapterNo) {
                        appDatabase.downloadbooksRecorddao().update(
                            pair.first.toString(),
                            result.identifier.toString()
                        )
                        trySend(ResponseDownload.Success(file.absolutePath))
                    }*//*
                }
            }

            override fun onProgress(data: DownloadData, progress: Float) {
//                Log.e("in progress", "$progress")

                Log.e("FileStatus", "downlodingProgress->$progress")

                ResponseDownload.OnDownload(
                        "100",
                        "$progress"
                    )



            }
        })

    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}