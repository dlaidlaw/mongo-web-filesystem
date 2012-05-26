package ca.donlaidlaw.mongo.webfs.filesystem

import com.mongodb.BasicDBObject
import com.mongodb.BasicDBObjectBuilder
import com.mongodb.DBObject
import com.mongodb.MongoException
import com.mongodb.gridfs.GridFS
import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSFile
import org.bson.types.ObjectId

import javax.annotation.PostConstruct

import ca.donlaidlaw.mongo.webfs.*
import ca.donlaidlaw.mongo.webfs.search.Page
import ca.donlaidlaw.mongo.webfs.search.FileSearchConditions
import ca.donlaidlaw.mongo.webfs.search.Result

class GridFSFilesystemService implements FilesystemService {

    public static final String TENANT = "tenant";

    /** GridFS database */
    GridFS gridFS

    FileMetadataMapper mapper = new FileMetadataMapper()

    @PostConstruct
    void init() {
        def bucket = gridFS.bucketName

        // add some indexes

        final def filesCollection = gridFS.DB.getCollection("${bucket}.files")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder.start("metadata.tenant", 1).get(), "tenant")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder
                        .start("metadata.tenant", 1)
                        .add("metadata.owner", 1).get(),
                "owner")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder
                        .start("metadata.tenant", 1)
                        .add("metadata.tags", 1).get(),
                "tags")

        filesCollection.ensureIndex(
                BasicDBObjectBuilder
                        .start("metadata.tenant", 1)
                        .add("metadata.references", 1).get(),
                "references")
    }

    String getBucket() {
        return gridFS.bucketName
    }

    /**
     * Inserts new file to the GridFS.
     * @param metadata a file metadata.
     * @param content an input stream with new file content.
     * @return the database id of just added file.
     */
    String insertFile(FileMetadata metadata, InputStream content) {
        validateFile(metadata)

        GridFSFile gridFile = gridFS.createFile(content, metadata.fileName, false)
        gridFile.contentType = metadata.contentType
        fillMetadata(metadata, gridFile, false)

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

        metadata.md5Checksum = md5
    }

    /**
     * Updates the file metadata only.
     * This method doesn't update a file content or version, also it doesn't update creator and create date information.
     *
     * @param metadata the file metadata.
     * @return the file id
     */
    void updateFile(FileMetadata metadata) {
        validateFile(metadata, true)

        GridFSFile gridFile = gridFS.findOne(new ObjectId(metadata.fileId))

        checkFileExists(gridFile, metadata.fileId);
        checkFileAccess(gridFile, metadata.tenant)

        fillMetadata(metadata, gridFile, true)
        gridFile.save();
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
     * Find the file by id.
     *
     * @param tenant the tenant.
     * @param id the file id.
     * @return the found file information.
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

    /**
     * Find file metadata by id.
     *
     * @param tenant the tenant.
     * @param id the file id.
     * @return the found file metadata.
     * @throws FileNotFoundException if specified file was not found
     * @throws FileAccessDeniedException if user has no access to the specified file
     */
    FileMetadata getFileMetadata(String tenant, String id) {
        GridFSDBFile dbFile = gridFS.findOne(new ObjectId(id))

        checkFileExists(dbFile, id)
        checkFileAccess(dbFile, tenant)

        return mapper.read(dbFile)
    }

    /**
     * Search files by specified conditions.
     *
     * @param conditions the search conditions.
     * @param page the information about page that need to be retrieved.
     * @return the list of found files metadata.
     * @throws AccessDeniedException when tenant is not specified.
     */
    Result<FileMetadata> searchFiles(FileSearchConditions conditions, Page page) {
        if (!conditions.tenant) {
            throw new AccessDeniedException("tenant is required")
        }

        BasicDBObject query = buildSearchQuery(conditions)
        def bucket = gridFS.bucketName
        def filesCollection = gridFS.DB.getCollection("${bucket}.files")
        def files = filesCollection.find(query).skip((page.page - 1) * page.perPage).limit(page.perPage)
        def results = new Result<FileMetadata>(page: page.page, perPage: page.perPage)
        results.list = files.collect(mapper.&read)

        return results
    }

    /**
     * Build a search query.
     * NOTE: all subqueries are appended under AND clause.
     *
     * @param conditions the conditions to build a query.
     * @return the object that represents a query.
     */
    protected BasicDBObject buildSearchQuery(FileSearchConditions conditions) {
        def subQueries = [new BasicDBObject("metadata.$FileMetadataMapper.TENANT", conditions.tenant)]

        if (conditions.fileName) {
            subQueries << new BasicDBObject(FileMetadataMapper.FILENAME, conditions.fileName)
        }
        if (conditions.owner) {
            subQueries << new BasicDBObject("metadata.$FileMetadataMapper.OWNER", conditions.owner)
        }
        if (conditions.tag) {
            subQueries << new BasicDBObject("metadata.$FileMetadataMapper.TAGS", conditions.tag)
        }
        if (conditions.reference) {
            subQueries << new BasicDBObject("metadata.$FileMetadataMapper.REFERENCES", conditions.reference)
        }
        else if (conditions.emptyReference) {
            // return only files that haven't references
            subQueries << new BasicDBObject("metadata.$FileMetadataMapper.REFERENCES", new BasicDBObject('$size', 0))
        }
        if (conditions.classifier) {
            subQueries << new BasicDBObject("metadata.$FileMetadataMapper.CLASSIFIER", conditions.classifier)
        }

        return new BasicDBObject('$and', subQueries)
    }

    /**
     * Check if retrieved file actually exists.
     *
     * @param file the GridFS file.
     * @param fileId the file id.
     * @throws FileNotFoundException if file not found.
     */
    protected void checkFileExists(GridFSFile file, String fileId) {
        if (!file) {
            throw new FileNotFoundException("File with id $fileId was not found");
        }
    }

    /**
     * Check if user has access to specified file..
     *
     * @param file the GridFS file.
     * @param tenant the current tenant.
     * @throws FileAccessDeniedException if current tenant has not access to the file.
     */
    protected void checkFileAccess(GridFSFile file, String tenant) {
        if (tenant != null && file) {
            DBObject metadata = file.metaData
            if (metadata && tenant.equals(metadata[TENANT])) {
                return
            }
        }
        throw new FileAccessDeniedException(file.id.toString())
    }

    protected void validateFile(FileMetadata metadata, boolean update = false) {
        metadata.validate()

        /*
        TODO - currently modifiedBy can be null, uncomment as soon as security is enabled
        if (update) {
            if (!metadata.modifiedBy) {
                metadata.errors.rejectValue("modifiedBy", "nullable")
            }
        }*/

        if (metadata.hasErrors()) {
            throw new EntityValidationException();
        }
    }

    /**
     * File GridFS file with metadata on insert or update file.
     * As there is a limitation when fill metadata on update,
     * the argument {@code update} should be {@code true} when update metadata.
     *
     * @param metadata the file metadata.
     * @param gridFile the gridFS file.
     * @param update the flag, true if metadata is filled on update.
     */
    protected void fillMetadata(FileMetadata metadata, GridFSFile gridFile, boolean update) {
        if (update) {
            mapper.update(metadata, gridFile)
        }
        else {
            mapper.write(metadata, gridFile)
        }
    }

}
