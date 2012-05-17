package ca.donlaidlaw.mongo.webfs

/**
 * Represents a webfs file and contains both ref to file stream and metadata.
 */
class WebFSFile {

    final FileMetadata metadata
    final InputStream content

    WebFSFile(FileMetadata metadata, InputStream content) {
        this.metadata = metadata
        this.content = content
    }
}
