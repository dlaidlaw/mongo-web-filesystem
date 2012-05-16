package ca.donlaidlaw.mongo.webfs

/**
 * Contains both ref to file stream and metadata.
 */
class Document {

    final DocumentMetadata metadata
    final InputStream content

    Document(DocumentMetadata metadata, InputStream content) {
        this.metadata = metadata
        this.content = content
    }
}
