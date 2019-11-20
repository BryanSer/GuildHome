package com.github.bryanser.guildhome.transform

import java.io.ObjectInputStream
import java.io.Serializable
@Deprecated("转为使用JSON")
class Data(
        val path: String,
        private val data: ObjectInputStream,
        private val result:Any? = null,
        private val onClose: () -> Unit
) {

    fun isSerializable():Boolean = this.result != null

    fun <T:Serializable> load():T{
        return result as T
    }

    fun load(obj: Any) {
        if(result != null){
            throw IllegalStateException("这个对象是直接序列化的")
        }
        for (f in DataTransform.getAllField(obj.javaClass)) {
            f.isAccessible = true
            if (f.isAnnotationPresent(Transform::class.java)) {

                val type = f.type
                if (type.isPrimitive) {
                    when (type) {
                        Int::class.java -> f.setInt(obj, data.readInt())
                        Short::class.java -> f.setShort(obj, data.readShort())
                        Byte::class.java -> f.setByte(obj, data.readByte())
                        Boolean::class.java -> f.setBoolean(obj, data.readBoolean())
                        Long::class.java -> f.setLong(obj, data.readLong())
                        Float::class.java -> f.setFloat(obj, data.readFloat())
                        Double::class.java -> f.setDouble(obj, data.readDouble())
                        Char::class.java -> f.setChar(obj, data.readChar())
                    }
                } else  {
                    f.set(obj, data.readObject())
                }
            }
        }
    }

    fun close() {
        data.close()
        onClose()
    }
}