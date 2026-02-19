package com.example.phonotify.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream


object ApplicationsSerializer: Serializer<Applications> {
    override val defaultValue: Applications
        get() = Applications.getDefaultInstance()


    override suspend fun readFrom(input: InputStream): Applications {
        try {
            return Applications.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read application proto", e)
        }
    }

    override suspend fun writeTo(
        t: Applications,
        output: OutputStream
    ) {
        t.writeTo(output)
    }

}