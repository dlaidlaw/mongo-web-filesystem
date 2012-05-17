package ca.donlaidlaw.mongo.webfs

import grails.validation.Validateable

/**
 * The metadata for some document.
 */
@Validateable
class FileMetadata {

    String fileId

    String fileName
    long fileSize

    String classifier

    String md5Checksum

    String owner
    String tenant

    String uploadedBy
    String modifiedBy

    Date uploadDate

    Set<String> tags
    Set<String> references

    String contentType

    String note

    static constraints = {
        fileName blank: false, nullable: false

        tenant blank: false, nullable: false
        owner blank: true, nullable: true
        uploadedBy nullable: true, blank: true
        modifiedBy blank: true, nullable:  true
        uploadDate nullable: true

        tags nullable: true
        note nullable: true, blank: true
        references nullable: true
        classifier nullable: true, blank: true

        contentType nullable: true, blank: true
        md5Checksum nullable: true, blank: true
    }

}
