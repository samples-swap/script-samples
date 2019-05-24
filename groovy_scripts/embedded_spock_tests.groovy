package foo

@Grab('org.spockframework:spock-core:1.0-groovy-2.3')

import org.junit.runner.*

import spock.lang.Specification
import spock.util.EmbeddedSpecRunner

/*
 * output:
 * class: class org.junit.runner.Result
 * failures: [fail(apackage.MySpec): Condition not satisfied:
 *
 * thing.b == thing.a
 * |     | |  |     |
 * |     be|  |     ay
 * |       |  [a:ay, b:be]
 * |       false
 * |       2 differences (0% similarity)
 * |       (be)
 * |       (ay)
 * [a:ay, b:be]
 * ]
 * failureCount: 1
 * runTime: 11
 * runCount: 3
 * ignoreCount: 1
 *
 * ** done **
 */

EmbeddedSpecRunner runner = new EmbeddedSpecRunner(throwFailure: false)
String bodySrc = """
    def 'a is good'() {
        expect: thing.a == 'ay'
    }
    
    def 'b is good'() {
        expect: thing.b == 'be'
    }
    
    def 'fail'() {
        expect: thing.b == thing.a
    }
    
    @Ignore
    def 'ignored'() {
        expect: false
    }
"""
String src = "class MySpec extends foo.BaseSpec { ${bodySrc.trim() + '\n'} }"

Result result = runner.runWithImports(src)
result.properties.each { k, v -> println "$k: $v" }

class BaseSpec extends Specification {
    Map getThing() {
        return [a: 'ay', b: 'be']
    }
}

println '\n** done **'