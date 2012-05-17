package ca.donlaidlaw.mongo.webfs.controller

import ca.donlaidlaw.mongo.webfs.service.FilesystemService
import grails.converters.JSON
import grails.converters.XML

import javax.activation.MimeType

import ca.donlaidlaw.mongo.webfs.*

class FilesystemController extends AbstractRESTController {

    FilesystemService filesystemService

    def get() {
        if (params.id) {
            retrieveDocument()
        } else {
            searchDocuments()
        }
    }

    void retrieveDocument() {
        def document
        try {
            document = filesystemService.getFile(params.tenant, params.id)
        } catch (FileNotFoundException e) {
            response.sendError SC_NOT_FOUND
            return
        } catch (FileAccessDeniedException e) {
            response.sendError SC_FORBIDDEN
            return
        }

        response.setHeader("Content-disposition", "attachment; filename=${document.metadata.fileName}")
        response.contentType = document.metadata.contentType
        copyMetadataToResponse(document.metadata)

        response << document.content
        response.flushBuffer()
    }

    private void searchDocuments() {
        // TODO - Need to implement this method in the service
        def documents = filesystemService.findDocuments(request, params)
        withFormat {
            xml { render documents as XML }
            json { render documents as JSON }
        }
        render documents as JSON
    }

    /**
     * Will insert a file into the file store.
     * @return the id of the file as a string.
     */
    def insert() {
        def metadata = new FileMetadata()
        readMetadata(metadata)

        // custom metadata
        metadata.uploadedBy = request.userPrincipal?.name

        def contentType = request.contentType
        def content
        if (isFileUploaded(contentType)) {
            // This happens if the file is being uploaded with an upload form.
            def uploadFile = request.getFile('upload')
            metadata.fileName = metadata.fileName?: uploadFile.originalFileName
            metadata.contentType = uploadFile.contentType
            content = uploadFile.inputStream
        } else {
            // Just take the post content as the file content.
            metadata.contentType = contentType
            content = request.inputStream
        }

        try {
            String id = filesystemService.insertFile(metadata, content)
            render text: id.toString(), status: 200
        } catch (EntityValidationException e) {
            renderErrors(metadata)
        } catch (ValidationException e) {
            renderError(e)
        }
    }

    /**
     * This is a PUT request. We only update the metadata, not the file content.
     * @return the file id if the file was found.
     */
    def update() {
        def metadata = new FileMetadata()
        readMetadata(metadata)

        // custom metadata
        metadata.modifiedBy = request.userPrincipal?.name

        try {
            filesystemService.updateFile(metadata)
        } catch (EntityValidationException e) {
            renderErrors(metadata)
            return
        } catch (ValidationException e) {
            renderError(e)
            return
        } catch (FileNotFoundException e) {
            response.sendError SC_NOT_FOUND
            return
        } catch (FileAccessDeniedException e) {
            response.sendError SC_FORBIDDEN
            return
        }

        render status: SC_SUCCESS
    }

    def delete() {
        try {
            filesystemService.deleteFile(params.id, params.tenant)
        } catch (FileNotFoundException e) {
            response.sendError SC_NOT_FOUND
            return
        } catch (FileAccessDeniedException e) {
            response.sendError SC_FORBIDDEN
            return
        }

        render status: SC_SUCCESS
    }

    // helpers

    private def copyMetadataToResponse(FileMetadata metadata) {
        response.setHeader("WEBFS.ID", metadata.fileId);
        response.setHeader("WEBFS.NAME", metadata.fileName);
        response.setIntHeader("WEBFS.SIZE", metadata.fileSize.intValue());
        response.setHeader("WEBFS.MD5", metadata.md5Checksum);
        response.setDateHeader("WEBFS.UPLOAD_DATE", metadata.uploadDate.time);
        response.setHeader("WEBFS.TENANT", metadata.tenant);

        addOptionalHeader("WEBFS.UPLOADED_BY", metadata.uploadedBy)
        addOptionalHeader("WEBFS.MODIFIED_BY", metadata.modifiedBy)
        addOptionalHeader("WEBFS.CLASSIFIER", metadata.classifier)
        addOptionalHeader("WEBFS.OWNER", metadata.owner)
        addOptionalHeader("WEBFS.REFERENCES", metadata.references)
        addOptionalHeader("WEBFS.TAGS", metadata.tags)
        addOptionalHeader("WEBFS.NOTE", metadata.note)
    }

    private void addOptionalHeader(String headerName, String value) {
        if (value) {
            response.setHeader(headerName, value)
        }
    }

    private void addOptionalHeader(String headerName, Collection<String> value) {
        if (value) {
            value.each { response.addHeader(headerName, it) }
        }
    }

    private void readMetadata(final FileMetadata metadata) {
        metadata.fileId = params.id?.trim()
        metadata.fileName = params.name?.trim()
        metadata.tags = params.list('tags')
        metadata.references = params.list('references')
        metadata.md5Checksum = params.md5
        metadata.note = params.note?.trim()
        metadata.classifier = params.classifier?.trim()
        metadata.tenant = params.tenant
        metadata.owner = params.owner
    }

    private boolean isFileUploaded(String contentType) {
        if (contentType) {
            MimeType mimeType = new MimeType(contentType)
            return mimeType.match("multipart/form-data")
        }

        return false
    }

}
