package com.example.ble_con.fileManager

import android.content.Context
import android.util.Log
import com.example.phonotify.BuildConfig
import com.example.phonotify.ViewModelData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FileManager(
    val context:Context
) {
    private val TAG = "FileManager"
    private val recordingsFolderName = "/recordings/"
    private val recordingsPath = context.dataDir.path + recordingsFolderName
    private val recordingsFolder = getFolderFromName(recordingsFolderName)
    private val logFolderName = "/log/"
    private var logFileName = "log.txt"

    private var newLog = false
        set(value) {
            field = value
            scope.launch {
                ViewModelData.newLogs.emit(value)
            }
        }

    private val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm", Locale.getDefault())
    private val saveDateFormat = SimpleDateFormat("dd-MM-yyyy_HH:mm", Locale.getDefault())
    private val scope = CoroutineScope(Dispatchers.IO)

    val module = SerializersModule {
        //contextual(LatLng::class, LatLngSerializer)
        //contextual(Point::class, PointSerializer)
    }
    val json = Json {
        serializersModule = module
    }

    init{
        Timber.d("init...")

        if(!recordingsFolder.exists())
            Timber.d("creating folder${recordingsFolder} is ${recordingsFolder.mkdir()}")
        initLog()
    }
    fun initLog(): Boolean{
         val folder = getFolderFromName(logFolderName)
         if(!folder.exists()) Timber.d("creating folder${folder} is ${folder.mkdir()}")

         val file = getFileFromName(logFileName,logFolderName)
         if(!file.exists()){
            val ch = file.createNewFile()
            if(ch == false) {
                Timber.d("can't create file $logFolderName$logFileName")
                return false
            }
            Timber.d("Created new File $logFolderName$logFileName")
         }

        return true
    }
    fun log(priority:Int, tag:String, message:String, timeStamp:Boolean=true){
        val file = getFileFromName(logFileName,logFolderName)

        if(!file.exists()) Log.d("Timber","file doesn't exist")

        val priority_str = when(priority){
            Log.ERROR -> "ERROR"
            Log.WARN -> "WARNING"
            else -> "INFO"
        }
        scope.launch{
            val str = when{
                timeStamp -> {
                    val curDate = Calendar.getInstance()
                    val time = saveDateFormat.format(curDate.time)
                    "$time-$priority_str-$tag: $message\n"
                }
                else -> "$priority_str-$tag: $message\n"
            }
            appendText(file,str)
            newLog = true
        }
    }
    fun getLogs(): String{
        checkLogFileSize()
        val file = getFileFromName(logFileName,logFolderName)
        return readStr(file)
    }
    fun clearLogs(){
        val file = getFileFromName(logFileName,logFolderName)
        write(file,"")
        newLog = true
    }
    fun checkLogFileSize(){
        val limit_file_size_KB = 100
        val min_file_size_KB = 60

        val file = getFileFromName(logFileName,logFolderName)

        if (file.length()/1024 >= limit_file_size_KB){
            val remove_size = file.readBytes().size - min_file_size_KB*1024
            val bytes = file.readBytes().drop(remove_size).toByteArray()
            write(file,bytes)
        }
        //Log.d("TIMBER","${file.length()/1024} read..${ file.readBytes().size}")
    }
    private fun getFileFromName(fileName: String,folder:String = recordingsFolderName): File{
        return File(context.dataDir.toString()+ folder,fileName)
    }
    private fun getFolderFromName(folderName: String): File{
        return File(context.dataDir.toString(),folderName)
    }
    private fun getFileDataFromName(fileName: String): FileData{
        val file = getFileFromName(fileName)

        val curDate = Calendar.getInstance()
        curDate.timeInMillis = file.lastModified()
        val str = dateFormat.format(curDate.time)

       return FileData(fileName,str)
    }
    fun delete(fileName:String){
        val file = getFileFromName(fileName)
        Timber.d("deleting file $fileName ${file.delete()}")
        notifyViewModelData()
    }
    fun readAll(file: File): String{
        //val outFile = File(context.dataDir.toString()+ recordingsFolderName,fileName)
        if (file.isDirectory || !file.canRead()) {
            Timber.e("Cant read ${file.name} is a nonReadable or a directory")
            return "Error"
        }
        var content = file.reader().use{it.readText()}
        /*
        context.openFileInput(fileName).bufferedReader().useLines { lines ->
            content += lines.fold("") { some, text ->
                "$some\n$text"
            }
        }
        */
        return content
    }
    private fun appendText(file:File,str: String){
        file.appendText(str)
    }
    private fun write(file:File,str:String){
        file.writeBytes(str.toByteArray())
    }
    private fun write(file:File,bytes: ByteArray){
        file.writeBytes(bytes)
    }
    private fun readStr(file:File): String{
        return file.readBytes().toString(Charsets.UTF_8)
    }
    fun save(str: String): Boolean{

        var fileName = str
        if(fileName == "") {
            val curDate = Calendar.getInstance()
            fileName = saveDateFormat.format(curDate.time)
        }
        val file = getFileFromName(fileName)
        if(!file.exists()){
            val ch = file.createNewFile()
            if(ch == false) {
                Timber.d("can't create file $fileName")
                return false
            }
            Timber.d("Created new File $fileName")
        }
        else{
            Timber.d("file $fileName already exists")
            return false
        }
        scope.launch {
            val dataBundle = ""
            /*
            val dataBundle = SensorDataBundle(
                temperature = SensorData.temperature.getList(),
                humidity = SensorData.humidity.getList(),
                pressure = SensorData.pressure.getList(),
                iaq = SensorData.iaq.getList(),
                voc = SensorData.voc.getList(),
                co2 = SensorData.co2.getList(),
                steps = SensorData.steps.getList(),
                altitude = SensorData.altitude.getList(),
                location = SensorData.location.getList()
            )
             */
            val json = json.encodeToJsonElement(dataBundle).toString()
            write(file, json)
            notifyViewModelData()

        }
        return true
    }

    fun getFileList(): List<FileData>{
        val list = mutableListOf<FileData>()
        val folder = recordingsFolder.list()

        folder?.forEach{fileName->
            list.add(getFileDataFromName(fileName))
        }

        return list.toList()
    }

     fun loadFile(fileName: String){
        scope.launch {
            val file = getFileFromName(fileName)

           // ViewModelData.fileData = getFileDataFromName(fileName)

            /*
            val out = json.decodeFromString<SensorDataBundle>(readAll(file))
            Timber.d("$out")

            SensorData.clearData()
            out.temperature.forEach { SensorData.temperature.add(it) }
            out.humidity.forEach { SensorData.humidity.add(it) }
            out.pressure.forEach { SensorData.pressure.add(it) }
            out.steps.forEach { SensorData.steps.add(it) }
            out.iaq.forEach { SensorData.iaq.add(it) }
            out.voc.forEach { SensorData.voc.add(it) }
            out.co2.forEach { SensorData.co2.add(it) }
            out.altitude.forEach { SensorData.altitude.add(it) }
            out.location.forEach { SensorData.location.add(it) }
         */
        }
    }
    fun notifyViewModelData(){
     //   ViewModelData.updateFileList(getFileList())
    }
}