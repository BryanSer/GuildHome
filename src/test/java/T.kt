import com.github.bryanser.guildhome.StringManager
import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.transform.DataTransform
import com.github.bryanser.guildhome.transform.Transform
import com.google.gson.Gson
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.Serializable

@Ignore
class T {

    @Test
    fun testJson() {
        val data = mutableMapOf<String, Any>()
        val sub = mutableMapOf<String, Any>()
        sub["test"] = "test"
        sub["value"] = 1
        println("sub: $sub")
        data["sub"] = sub
        data["version"] = "1.0"
        val json = StringManager.toJson(data)
        println("jons: \n$json")
        val result = StringManager.fromJson(json)
        println("result: $result")
        val subm = result["sub"] as Map<String, Any>
        println("sub: $subm")
        Assert.assertEquals(sub.toString(), subm.toString())
    }

    @Test
    fun testUntil() {
        Assert.assertFalse(1 in 1 until 1)

    }


    @Test
    fun career() {
        Assert.assertTrue(Career.VP >= Career.MANAGER)
    }
//
//    @Test
//    fun it(){
//        inlineTest {
//            println(1)
//            return
//        }
//        println(2)
//    }
//
//    inline fun inlineTest(func:()->Unit){
//        try{
//            func()
//        }finally {
//            println("finally")
//        }
//    }

    @Test
    fun str() {
        val s = String::class.java
        val cs = CharSequence::class.java
        Assert.assertTrue(cs.isAssignableFrom(s))
    }

}