package com.github.bryanser.guildhome

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.*


object StringManager {
    val gson = Gson()
    val type = object: TypeToken<Map<String,Any?>>(){}.type
    val parseGson = GsonBuilder().registerTypeAdapter(type, GsonTypeAdapter()).create()

    fun toJson(map: Map<String, Any>): String = gson.toJson(map)


    @Suppress("UNCHECKED_CAST")
    fun fromJson(json: String): Map<String, Any> = parseGson.fromJson(json, type) as Map<String, Any>

    class GsonTypeAdapter : TypeAdapter<Any>() {
        override fun write(out: JsonWriter?, value: Any?) {
        }

        override fun read(jr: JsonReader): Any? {
            val token: JsonToken = jr.peek()
            return when (token) {
                JsonToken.BEGIN_ARRAY -> {
                    val list: MutableList<Any?> = ArrayList()
                    jr.beginArray()
                    while (jr.hasNext()) {
                        list.add(read(jr))
                    }
                    jr.endArray()
                    list
                }
                JsonToken.BEGIN_OBJECT -> {
                    val map: MutableMap<String, Any?> = HashMap()
                    jr.beginObject()
                    while (jr.hasNext()) {
                        map[jr.nextName()] = read(jr)
                    }
                    jr.endObject()
                    map
                }
                JsonToken.STRING -> jr.nextString()
                JsonToken.NUMBER -> {
                    jr.nextInt()
                }
                JsonToken.BOOLEAN -> jr.nextBoolean()
                JsonToken.NULL -> {
                    jr.nextNull()
                    null
                }
                else -> throw IllegalStateException()
            }
        }

    }
}