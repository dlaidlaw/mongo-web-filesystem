package ca.donlaidlaw.mongo.webfs.service

import com.mongodb.BasicDBObjectBuilder
import com.mongodb.DB
import com.mongodb.DBObject
import com.mongodb.MongoException
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSFile
import org.bson.types.ObjectId

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

import ca.donlaidlaw.mongo.webfs.*

class FilesystemService {

    public static final String TENANT = "tenant";

    /** Database */
    DB db
    /** GridFS database */
    GridFS gridFS

    String bucket = "fs"

    FileMetadataMapper mapper = new FileMetadataMapper()

    @PostConstruct
    void init() {
        gridFS = new GridFS(db, bucket)

        // add some indexes

        final def filesCollection = gridFS.DB.getCollection("${bucket}.files")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder
                        .start("metadata.tenant", 1)
                        .add("metadata.tags", 1).get(),
                "tags")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder.start("metadata.tenant", 1).get(), "tenant")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder
                        .start("metadata.tenant", 1)
                        .add("metadata.references", 1).get(),
                "related")
    }

    // TODO - what if failed to save to database? MongoException?
    String insertFile(FileMetadata metadata, InputStream content) {
        validateFile(metadata)

        GridFSFile gridFile = gridFS.createFile(content, metadata.fileName, false)
        gridFile.contentType = metadata.contentType
        gridFile = mapper.write(metadata, gridFile)

        gridFile.save();

        validateSavedFile(gridFile, metadata)
        metadata.fileId = gridFile.id.toString()
        metadata.fileSize = gridFile.length

        return metadata.fileId
    }

    private void validateSavedFile(GridFSFile gridFile, FileMetadata metadata) {
        // TODO - check md5 without transferring to GridFS

        try {
            // check if file was transferred correct
            gridFile.validate()
        } catch (MongoException e) {
            gridFS.remove((ObjectId) gridFile.id)
            throw new InvalidMD5ChecksumException()
        }

        // validate md5
        def md5 = gridFile.MD5
        if (metadata.md5Checksum && metadata.md5Checksum != md5) {
            gridFS.remove((ObjectId) gridFile.id)
            throw new InvalidMD5ChecksumException()
        }

        // if correct, then update docMetadata MD5 checksum
        metadata.md5Checksum = md5
    }

    def updateFile(FileMetadata metadata) {
        validateFile(metadata, true)

        GridFSFile gridFile = gridFS.findOne(new ObjectId(metadata.fileId))

        checkFileExists(gridFile, metadata.fileId);
        checkFileAccess(gridFile, metadata.tenant)

        gridFile = mapper.update(metadata, gridFile)
        gridFile.save();
        return gridFile;
    }

    /**
     * Delete file by id.
     *
     * @param fileId the file id.
     * @param tenant the tenant.
     */
    void deleteFile(String fileId, String tenant) {
        final def objectId = new ObjectId(fileId)
        final GridFSDBFile gridFile = gridFS.findOne(objectId)

        checkFileExists(gridFile, fileId);
        checkFileAccess(gridFile, tenant);

        gridFS.remove(objectId)
    }

    /**
     * Find the document by id.
     *
     * @param tenant the tenant.
     * @param id the document id.
     * @return the found document information.
     * @throws FileNotFoundException if specified file was not found
     * @throws FileAccessDeniedException if user has no access to the specified file
     */
    WebFSFile getFile(String tenant, String id) {
        GridFSDBFile dbFile = gridFS.findOne(new ObjectId(id))

        checkFileExists(dbFile, id)
        checkFileAccess(dbFile, tenant)

        def metadata = mapper.read(dbFile)
        return new WebFSFile(metadata, dbFile.inputStream);
    }

    def findFile(HttpServletRequest request, Map<String, Object> params) {
        // TODO - implement me
    }

    /*def setFileMetadataFromParams(GridFSFile file, Map<String, Object> params, boolean isInsert) {
        DBObject metadata = file.getMetaData()
        if (metadata == null) {
            metadata = new BasicDBObject()
            file.setMetaData(metadata)
        }
        metadata.put(TENANT, params[TENANT])
        if (isInsert) {
            if (params[UPLOADED_BY]) metadata.put(UPLOADED_BY, params[UPLOADED_BY])
        } else {
            if (params[MODIFIED_BY]) metadata.put(MODIFIED_BY, params[MODIFIED_BY])
        }
        metadata.put(CLASSIFIER, params[CLASSIFIER])
        metadata.put(OWNER, params[OWNER])
        metadata.put(REFERENCES, params.list(REFERENCES))
        metadata.put(TAGS, params.list(TAGS))
    }
*/

    private void checkFileExists(GridFSDBFile file, String fileId) {
        if (!file) {
            throw new FileNotFoundException("File with id $fileId was not found");
        }
    }

    private void checkFileAccess(GridFSFile file, String tenant) {
        if (tenant != null && file) {
            DBObject metadata = file.metaData
            if (metadata && tenant.equals(metadata[TENANT])) {
                return
            }
        }
        throw new FileAccessDeniedException(file.id.toString())
    }

    private void validateFile(FileMetadata metadata, boolean update = false) {
        metadata.validate()

        if (update) {
            if (!metadata.modifiedBy) {
                metadata.errors.rejectValue("modifiedBy", "nullable")
            }
        }

        if (metadata.hasErrors()) {
            throw new EntityValidationException();
        }
    }

    // TODO - move this functionality to controller

    /*
      * We will not track versions of files with an auto-increment counter. Versions
      * will be derived from insert order only. So we can think of the _id of the
      * file to be a version identifier. The _id is only accurate to the nearest second
      * for ordering, so maybe use the uploaded date/time instead.
     def getNextFileVersion(String fileName) {
         DBCollection coll = db.getCollection("${bucket}.versions")
         DBObject query = new BasicDBObject("_id", fileName)
         DBObject sort = null
         DBObject fields = null
         DBObject update = new BasicDBObject("\$inc", new BasicDBObject("version", new Long(1)))
 //		findAndModify(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, boolean, com.mongodb.DBObject, boolean, boolean)
 //		def result = coll.findAndModify(query, fields, sort, Boolean.FALSE, update, Boolean.TRUE, Boolean.TRUE)
         def result = coll.findAndModify(query, update)
         if (result == null) {
             query.put("version", new Long(1))
             coll.insert(query, WriteConcern.SAFE)
             return 0L
         }
         return result.version;
     }
     */

    /*
      * We are not trying to emulate a computer filesystem. There are no normalized
      * file names here. Files may have any string for a name, the interpretation of
      * a hierarchy, or any other structure of files is up to the user.
     String makeGridFSFilename(String name) {
         String filename = name
         if (!filename.startsWith("/")) filename = root + "/" + filename
         else filename = root + filename
         return filename
     }
     */
}
