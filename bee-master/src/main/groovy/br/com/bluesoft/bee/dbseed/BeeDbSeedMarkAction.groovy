package br.com.bluesoft.bee.dbseed

import br.com.bluesoft.bee.dbseed.DbSeedManager
import br.com.bluesoft.bee.runner.ActionRunnerParameterValidate

class BeeDbSeedMarkAction extends ActionRunnerParameterValidate {
	boolean run() {
		def migrationId = options.arguments[1]
		new DbSeedManager(configFile: options.configFile, path: options.dataDir.absolutePath, clientName: options.arguments[0], logger: out).mark(migrationId)
		true
	}

	@Override
	int minParameters() {
		return 2
	}
}
