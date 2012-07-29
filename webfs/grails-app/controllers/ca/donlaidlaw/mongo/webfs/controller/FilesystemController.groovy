package ca.donlaidlaw.mongo.webfs.controller

import ca.donlaidlaw.mongo.webfs.filesystem.FilesystemService
import grails.converters.JSON
import grails.converters.XML

import javax.activation.MimeType

import ca.donlaidlaw.mongo.webfs.*
import ca.donlaidlaw.mongo.webfs.search.Page
import ca.donlaidlaw.mongo.webfs.search.FileSearchConditions

class FilesystemController extends AbstractRESTController {

    static allowedMethods = [get: 'GET', insert: 'POST', update: 'PUT', delete: 'DELETE']

    FilesystemService filesystemService

    /**
     * Get file by id or in search.
     */
    def get() {
        try {
            if (params.id) {
                retrieveFile()
            } else {
                searchFiles()
            }
        } catch (AccessDeniedException e) {
            log.error("Access denied error: " + e.message)
            response.sendError SC_FORBIDDEN
        }
    }

    protected void retrieveFile() {
        def file
        try {
            file = filesystemService.getFile(params.tenant, params.id)
        } catch (FileNotFoundException e) {
            response.sendError SC_NOT_FOUND
            return
        }

        response.setHeader("Content-Disposition", "attachment; filename=${file.metadata.fileName}")
        response.contentType = file.metadata.contentType
        response.contentLength = file.metadata.fileSize
        copyMetadataToResponse(file.metadata)

        response.outputStream << file.content
        response.flushBuffer()
    }

    protected void searchFiles() {
        def searchConditions = readSearchConditions()
        def page = readPage()
        def filesMetadata = filesystemService.searchFiles(searchConditions, page)

        withFormat {
            xml { render filesMetadata.list as XML }
            json { render filesMetadata.list as JSON }
        }
    }

    /**
     * Will insert a file into the file store.
     * @return the id of the file as a string and @return 200 if created
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
            metadata.fileName = metadata.fileName ?: uploadFile.originalFileName
            metadata.contentType = uploadFile.contentType
            content = uploadFile.inputStream
        } else {
            // Just take the post content as the file content.
            metadata.contentType = contentType
            content = request.inputStream
        }

        try {
            String id = filesystemService.insertFile(metadata, content)
            render text: id.toString(), status: SC_SUCCESS
        } catch (EntityValidationException e) {
            renderErrors(metadata)
        } catch (ValidationException e) {
            renderError(e)
        }
    }

    /**
     * We only update the metadata, not the file content.
     * @return 200 if updated, 403 if no access, 404 if file not found.
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
        } catch (AccessDeniedException e) {
            response.sendError SC_FORBIDDEN
            return
        }

        render status: SC_SUCCESS
    }

    /**
     * Delete file by id.
     * @return 200 if removed, 403 if no access, 404 if file not found.
     */
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

    protected void copyMetadataToResponse(FileMetadata metadata) {
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

    private FileSearchConditions readSearchConditions() {
        def conditions = new FileSearchConditions()

        conditions.tenant = params.tenant
        conditions.tag = params.tag?.trim()
        conditions.owner = params.owner?.trim()
        conditions.fileName = params.name?.trim()
        conditions.reference = params.reference?.trim()
        conditions.classifier = params.classifier?.trim()

        return conditions
    }

    private Page readPage() {
        def page = new Page()

        // default page is 1
        page.page = params.int('page') ?: 1

        // default items per page is 10
        page.perPage = params.int("items") ?: 10

        return page
    }
}
