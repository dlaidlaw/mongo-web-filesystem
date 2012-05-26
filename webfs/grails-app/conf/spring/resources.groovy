import ca.donlaidlaw.mongo.webfs.filesystem.GridFSFilesystemService
import com.mongodb.gridfs.GridFS

beans = {

    db(mongo: "getDB", application.config.grails.mongo.databaseName)

    gridFS(GridFS, db, "fs")

    filesystemService(GridFSFilesystemService) {
        gridFS = ref('gridFS')
    }
}
