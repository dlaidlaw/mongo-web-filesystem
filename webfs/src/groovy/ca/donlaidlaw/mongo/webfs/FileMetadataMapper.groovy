package ca.donlaidlaw.mongo.webfs

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSFile

/**
 * This is mapper from file metadata to GridFS file and vice versa.
 */
class FileMetadataMapper {

    static final String TENANT = "tenant"
    static final String OWNER = "owner"
    static final String UPLOADED_BY = "uploadedBy"
    static final String MODIFIED_BY = "modifiedBy"
    static final String CLASSIFIER = "classifier"
    static final String NOTE = "note"
    static final String TAGS = "tags"
    static final String REFERENCES = "references"
    static final String FILENAME = "filename"

    // ------------------------------------------- Read

    /**
     * Read the metadata of GridFS file.
     *
     * @param file the GridFS file that represents metadata.
     * @return the read file metadata.
     */
    FileMetadata read(final GridFSFile file) {
        final def result = new FileMetadata()

        result.fileId = file.id.toString()
        result.fileName = file.filename
        result.fileSize = file.length
        result.md5Checksum = file.MD5
        result.uploadDate = file.uploadDate
        result.contentType = file.contentType

        def fileMetadata = file.metaData
        result.tenant = fileMetadata.get(TENANT)
        result.owner = fileMetadata.get(OWNER)
        result.classifier = fileMetadata.get(CLASSIFIER)
        result.tags = readSetOfStrings((List) fileMetadata.get(TAGS))
        result.note = fileMetadata.get(NOTE)
        result.references = readSetOfStrings((List) fileMetadata.get(REFERENCES))
        result.uploadedBy = fileMetadata.get(UPLOADED_BY)
        result.modifiedBy = fileMetadata.get(MODIFIED_BY)

        return result
    }

    private Set<String> readSetOfStrings(List list) {
        new HashSet<String>(list)
    }

    // ------------------------------------------- Write

    /**
     * Write file metadata to gridFS file
     * @param fileMetadata the file metadata.
     * @param file the GridFS file.
     * @return the GridFS file with metadata
     */
    GridFSFile write(final FileMetadata fileMetadata, final GridFSFile file) {
        DBObject metadata = file.metaData

        if (metadata == null) {
            metadata = new BasicDBObject()
            file.metaData = metadata
        }

        metadata.put(OWNER, fileMetadata.owner)
        metadata.put(TENANT, fileMetadata.tenant)
        metadata.put(UPLOADED_BY, fileMetadata.uploadedBy)
        metadata.put(MODIFIED_BY, fileMetadata.modifiedBy)
        metadata.put(CLASSIFIER, fileMetadata.classifier)
        metadata.put(TAGS, fileMetadata.tags)
        metadata.put(NOTE, fileMetadata.note)
        metadata.put(REFERENCES, fileMetadata.references)

        return file
    }

    /**
     * Write file metadata to gridFS file
     * @param fileMetadata the file metadata.
     * @param file the GridFS file.
     * @return the GridFS file with metadata
     */
    GridFSFile update(final FileMetadata fileMetadata, final GridFSFile file) {
        DBObject metadata = file.metaData

        if (metadata == null) {
            metadata = new BasicDBObject()
            file.metaData = metadata
        }

        file.put(FILENAME, fileMetadata.fileName)

        metadata.put(OWNER, fileMetadata.owner)
        metadata.put(MODIFIED_BY, fileMetadata.modifiedBy)
        metadata.put(CLASSIFIER, fileMetadata.classifier)
        metadata.put(TAGS, fileMetadata.tags)
        metadata.put(NOTE, fileMetadata.note)
        metadata.put(REFERENCES, fileMetadata.references)

        return file
    }
}
