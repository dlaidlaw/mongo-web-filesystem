package ca.donlaidlaw.mongo.webfs

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.gridfs.GridFSFile

/**
 * This is mapper from document metadata to GridFS file and vice versa.
 */
class DocumentMetadataMapper {

    static final String ID = "id"
    static final String TENANT = "tenant"
    static final String OWNER = "owner"
    static final String UPLOADED_BY = "uploadedBy"
    static final String MODIFIED_BY = "modifiedBy"
    static final String CLASSIFIER = "classifier"
    static final String TAGS = "tags"
    static final String NOTE = "note"
    static final String REFERENCES = "references"

    // ------------------------------------------- Read

    /**
     * Read the metadata of GridFS file.
     *
     * @param document the GridFS file that represents metadata.
     * @return the read document metadata.
     */
    DocumentMetadata read(final GridFSFile document) {
        final def result = new DocumentMetadata()
        result.documentId = document.id.toString()
        result.fileName = document.filename
        result.fileSize = document.length
        result.md5Checksum = document.MD5
        result.uploadDate = document.uploadDate
        result.contentType = document.contentType

        def documentMetadata = document.metaData
        result.tenant = documentMetadata.get(TENANT)
        result.owner = documentMetadata.get(OWNER)
        result.classifier = documentMetadata.get(CLASSIFIER)
        result.tags = readSetOfStrings((List) documentMetadata.get(TAGS))
        result.note = documentMetadata.get(NOTE)
        result.references = readSetOfStrings((List) documentMetadata.get(REFERENCES))
        result.uploadedBy = document.get(UPLOADED_BY)
        result.modifiedBy = document.get(MODIFIED_BY)

        return result
    }

    private Set<String> readSetOfStrings(List list) {
        new HashSet<String>(list)
    }

    // ------------------------------------------- Write

    /**
     * Write document metadata to gridFS file
     * @param docMetadata the document metadata.
     * @param file the GridFS file.
     * @return the GridFS file with metadata
     */
    GridFSFile write(final DocumentMetadata docMetadata, final GridFSFile file) {
        DBObject metadata = file.metaData

        if (metadata == null) {
            metadata = new BasicDBObject()
            file.metaData = metadata
        }

        metadata.put(OWNER, docMetadata.owner)
        metadata.put(TENANT, docMetadata.tenant)
        metadata.put(UPLOADED_BY, docMetadata.uploadedBy)
        metadata.put(MODIFIED_BY, docMetadata.modifiedBy)
        metadata.put(CLASSIFIER, docMetadata.classifier)
        metadata.put(TAGS, docMetadata.tags)
        metadata.put(NOTE, docMetadata.note)
        metadata.put(REFERENCES, docMetadata.references)

        return file
    }

    /**
     * Write document metadata to gridFS file
     * @param docMetadata the document metadata.
     * @param file the GridFS file.
     * @return the GridFS file with metadata
     */
    GridFSFile update(final DocumentMetadata docMetadata, final GridFSFile file) {
        DBObject metadata = file.metaData

        if (metadata == null) {
            metadata = new BasicDBObject()
            file.metaData = metadata
        }

        //metadata.put(UPLOADED_BY, docMetadata.uploadedBy)
        metadata.put(MODIFIED_BY, docMetadata.modifiedBy)
        metadata.put(CLASSIFIER, docMetadata.classifier)
        metadata.put(TAGS, docMetadata.tags)
        metadata.put(NOTE, docMetadata.note)
        metadata.put(REFERENCES, docMetadata.references)

        return file
    }
}
