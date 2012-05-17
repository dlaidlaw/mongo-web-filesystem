environments {
    development {
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
    }
    test {
        grails {
            mongo {
                host = "mongodb"
                port = 27017
                databaseName = "webfs_test"
                options {
                    autoConnectRetry = true
                    connectTimeout = 300
                }
            }
        }
    }
    production {
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
    }
}