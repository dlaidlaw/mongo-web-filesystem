// Place your Spring DSL code here
beans = {

    db(mongo: "getDB", application.config.grails.mongo.databaseName)
}
