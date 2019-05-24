/Problem: 
//languages like tcl can simply switch between the String representation of a map or list and the
//object itself. In Groovy, this could theoretically work, but it doesn't:
def list1 = [1,2,3]
def list2 = ["1, 2, 3"]
assert         list1             !=         list2
assert         list1.toString()  ==         list2.toString()
assert Eval.me(list1.toString()) instanceof List
assert Eval.me(list2.toString()) instanceof List
assert Eval.me(list1.toString()) == Eval.me(list2.toString())

//Solution:
//Groovy has several ways to serialize a map or list. One popular way it to convert it to JSON
//http://groovy-lang.org/json.html
import groovy.json.*
def map = [
            list:[1,2,3],
            integer: 5,
            bigDecimal: 3.1415,
            string: "yes",
            boolean: true,
            //date: new Date(0)
          ]
def mapAsJson = JsonOutput.toJson(map)
assert mapAsJson == '{"list":[1,2,3],"integer":5,"bigDecimal":3.1415,"string":"yes","boolean":true}'
assert new JsonSlurper().parseText(mapAsJson) instanceof Map
assert new JsonSlurper().parseText(mapAsJson) == map

//as you can see, the entry with type "Date" is commented out. Unfortunately, the conversion isn't
//bi-directional for all data types

//another popular way to serialze a map is YAML. YAML has the slight drawback that it needs an 
//external library:
@Grab("org.yaml:snakeyaml:1.16")
import org.yaml.snakeyaml.Yaml

//def mapAsYaml = new Yaml().load(map) 
def mapAsYaml = new Yaml().dump(map)
assert mapAsYaml == """list: [1, 2, 3]
integer: 5
bigDecimal: 3.1415
string: 'yes'
boolean: true
"""
assert new Yaml().load(mapAsYaml) instanceof Map
assert new Yaml().load(mapAsYaml) == map

//Both ways work fine. The main difference is that the String representation of JSON is
//one line by default where YAML is distributed over several lines.

//In order to make these serializations easier to use, we can make use of metaprogramming:
String.metaClass.toMap {->
    new JsonSlurper().parseText(delegate)
}
assert '{"list":[1,2,3],"integer":5,"bigDecimal":3.1415,"string":"yes","boolean":true}'.toMap() == map

//Unfortunately, the toString() can't be overwritten:
Map.metaClass.toString {->
    JsonOutput.toJson(delegate)
}
assert '{"list":[1,2,3],"integer":5,"bigDecimal":3.1415,"string":"yes","boolean":true}'         != map.toString()

//But a toJson() method can be created:
Map.metaClass.toJson {->
    JsonOutput.toJson(delegate)
}
assert '{"list":[1,2,3],"integer":5,"bigDecimal":3.1415,"string":"yes","boolean":true}'         == map.toJson()

//btw: since YAML is a superset of JSON, you can deserialize JSON with YAML:
assert new Yaml().load(mapAsJson) == map
//but not the other way around.

//Groovy version used: 2.4.5