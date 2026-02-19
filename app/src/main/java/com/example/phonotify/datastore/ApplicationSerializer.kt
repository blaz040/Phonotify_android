package com.example.phonotify.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
//
//object ApplicationSerializer: Serializer<Application> {
//    override val defaultValue: Application
//        get() = Application.getDefaultInstance()
//
//    override suspend fun readFrom(input: InputStream): Application {
//        try {
//            return Application.parseFrom(input)
//        } catch (e: InvalidProtocolBufferException) {
//            throw CorruptionException("Cannot read application proto", e)
//        }
//    }
//
//    override suspend fun writeTo(
//        t: Application,
//        output: OutputStream
//    ) {
//        t.writeTo(output)
//    }
//
//}