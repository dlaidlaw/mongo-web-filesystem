grails {
    mongo {
        host = "mongodb"
        port = 27017
        databaseName = "webfs"
		options {
			autoConnectRetry = true
			connectTimeout = 300
		}
    }
}