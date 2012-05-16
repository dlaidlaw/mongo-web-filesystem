package ca.donlaidlaw.mongo.webfs

import grails.validation.Validateable

/**
 * The metadata for some document.
 */
@Validateable
class DocumentMetadata {

    String documentId

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
        tenant blank: false, nullable: false
        fileName blank: false, nullable: false
        owner blank: false, nullable: false
        uploadDate nullable: false
        modifiedBy blank: false, nullable:  false
        uploadDate nullable: true

        tags nullable: true
        note nullable: true, blank: true
        references nullable: true
    }

}
