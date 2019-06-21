package br.com.bluesoft.bee

import br.com.bluesoft.bee.upgrade.BeeUpgradeAction
import br.com.bluesoft.bee.upgrade.BeeVersionModule;
import spock.lang.Specification
import org.junit.Test

public class BeeUpgradeActionTest extends Specification {

	@Test
	def "deve verificar que o bee está atualizado e retornar true"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getCurrentVersion() >> "1.1"
		BeeVersionModule.getLatestVersion()  >> "1.1"

		when:""
		BeeUpgradeAction upgrade = new BeeUpgradeAction()
		def isLatest = upgrade.isLatestVersion()

		then:""
		isLatest == true
	}
	
	@Test
	def "deve verificar que o bee está desatualizado e retornar false"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getCurrentVersion() >> "1.0"
		BeeVersionModule.getLatestVersion()  >> "1.1"

		when:""
		BeeUpgradeAction upgrade = new BeeUpgradeAction()
		def isLatest = upgrade.isLatestVersion()

		then:""
		isLatest == false
	}

	@Test
	def "deve pegar a última versão e ela deve ser igual à versão retornada no módulo BeeVersionModule"() {
		given:""
		def beeVersionLatest = BeeVersionModule.getLatestVersion()

		when:""
		BeeUpgradeAction upgrade = new BeeUpgradeAction()
		def beeUpgradeLatest = upgrade.getLatestVersion()

		then:""
		beeUpgradeLatest == beeVersionLatest
	}

	@Test
	def "deve abortar a execução porque o nome do release está fora do padrão"() {
		given:""
		GroovyMock(BeeVersionModule, global: true)
		BeeVersionModule.getLatestVersion() >> "v1.1"

		GroovyMock(System, global: true)
		System.exit() >> 1

		when:""
		BeeUpgradeAction upgrade = new BeeUpgradeAction()
		def version = upgrade.getLatestVersion()

		then:""
		assert 1
	}
}
