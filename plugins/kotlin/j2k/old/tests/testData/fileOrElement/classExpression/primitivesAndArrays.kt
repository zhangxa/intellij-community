public object A {
    jvmStatic public fun main(args: Array<String>) {
        println(Void.TYPE)
        println(Integer.TYPE)
        println(java.lang.Double.TYPE)
        println(javaClass<IntArray>())
        println(javaClass<Array<Any>>())
        println(javaClass<Array<Array<Any>>>())
    }
}
