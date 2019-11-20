package com.github.bryanser.guildhome.transform

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.reflect.Field

@Deprecated("转为使用JSON")
object DataTransform {


    fun loadData(ba: ByteArray): Data {
        val bytein = ByteArrayInputStream(ba)
        val data = ObjectInputStream(bytein)
        val path = data.readUTF()
        if (data.readBoolean()) {
            return Data(path, data, data.readObject()) {
                bytein.close()
            }
        }
        return Data(path, data) {
            bytein.close()
        }
    }

    fun toDataByteArray(obj: Any, path: String): ByteArray {
        val cls = obj.javaClass
        val byteout = ByteArrayOutputStream()
        val data = ObjectOutputStream(byteout)
        data.writeUTF(path)
        if (obj is Serializable) {
            data.writeBoolean(true)
            data.writeObject(obj)
        } else {
            data.writeBoolean(false)
            for (f in getAllField(cls)) {
                f.isAccessible = true
                if (f.isAnnotationPresent(Transform::class.java)) {
                    val type = f.type
                    if (type.isPrimitive) {
                        when (type) {
                            Int::class.java -> data.writeInt(f.getInt(obj))
                            Short::class.java -> data.writeShort(f.getShort(obj).toInt())
                            Byte::class.java -> data.writeByte(f.getByte(obj).toInt())
                            Boolean::class.java -> data.writeBoolean(f.getBoolean(obj))
                            Long::class.java -> data.writeLong(f.getLong(obj))
                            Float::class.java -> data.writeFloat(f.getFloat(obj))
                            Double::class.java -> data.writeDouble(f.getDouble(obj))
                            Char::class.java -> data.writeChar(f.getChar(obj).toInt())
                        }
                    } else {
                        data.writeObject(f.get(obj))
                    }
                }
            }
        }
        data.flush()
        val ba = byteout.toByteArray()
        data.close()
        byteout.close()
        return ba
    }


    fun getAllField(cls: Class<in Any>, list: MutableList<Field> = mutableListOf()): MutableList<Field> {
        if (cls == Any::class.java) {
            return list
        }
        for (f in cls.declaredFields) {
            list += f
        }
        getAllField(cls.superclass, list)
        return list
    }

}