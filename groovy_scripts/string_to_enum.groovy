import org.codehaus.groovy.runtime.StringGroovyMethods

use(EnumUtils1) {
    println Foo.ONE.toString()
    println "THREE" as Foo
    println "123" as int
    println "Three" as Foo
    println "T" as Foo
}

enum Foo {
    ONE(null),
    TWO('T'),
    THREE('TT')
    
    String s
    Foo(String s) {
        this.s = s
    }
}

class EnumUtils1 {
    static <T> T asType(String self, Class<T> clazz) {
        if (clazz.isEnum()) {
            return clazz.values().find {
                it.name().toLowerCase() == self?.toLowerCase() || it.s == self
            }
        } else {
            StringGroovyMethods.asType(self, clazz)
        }
    }
}

enum Bar {
    ONE(null),
    TWO('T'),
    THREE('TT')
    
    String s
    Bar(String s) {
        this.s = s
    }
}

use(EnumUtils2) {
    println Bar.fromString('T')
}

class EnumUtils2 {
    static Enum fromString(Class<Enum> self, String s) {
        return self.values().find { it.s == s }
    }
}