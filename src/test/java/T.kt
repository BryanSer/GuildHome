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

}