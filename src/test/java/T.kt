import com.github.bryanser.guildhome.database.Career
import com.github.bryanser.guildhome.transform.DataTransform
import com.github.bryanser.guildhome.transform.Transform
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.Serializable

@Ignore
class T {

    @Test
    fun testUntil(){
        Assert.assertFalse(1 in 1 until 1)

    }


    @Test
    fun career(){
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

    @Test
    fun test() {
        val a = A()
        val arr = DataTransform.toDataByteArray(a, "test")
        val out = DataTransform.loadData(arr)
        val t = out.load<A>()
        out.close()
        println(t.i)
        println(t.b)
        println(t.test)
        println(t.s)
    }

    class A : Serializable {
        @Transform
        var i: Int = 10
        @Transform
        var b: String? = null
        @Transform
        var test: Int? = null
        @Transform
        var s = "helloworld"
    }
}