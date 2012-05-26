package ca.donlaidlaw.mongo.webfs.search

/**
 * A simple holder of file search conditions.
 *
 * @author Ruslan Khmelyuk
 */
class FileSearchConditions {

    String tenant

    String fileName
    String tag
    String classifier
    String owner

    String reference
    boolean emptyReference = false
}
