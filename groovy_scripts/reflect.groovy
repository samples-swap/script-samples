
class ReflectTest {
  def name
	def height

	def add(){
		println "add"
	}

	def add(a){
		println "add "+a
	}

	static main(args) {

		def show = {n->
			println "=================================="
			ReflectTest.metaClass."$n".each{ println it.name }
		}

		show("properties")
		show("methods")


		Class.metaClass.static.fqn = { delegate.name }
		
		String.metaClass.fqn << { println delegate }

		assert String.fqn() == "java.lang.String"
		"helo".fqn()

		ReflectTest.metaClass.invokeMethod  {   name,  gs ->
			def metaMethod = delegate.metaClass.getMetaMethod(name,  gs)
			if (metaMethod) {
				println "invoking " + name
				metaMethod.doMethodInvoke(delegate,  gs)
			} else {
				throw new MissingMethodException(name, delegate.class, gs)
			}
		}


		new ReflectTest().add()
	}
}
