package ca.donlaidlaw.mongo.webfs.controller

import ca.donlaidlaw.mongo.webfs.DocumentMetadata
import ca.donlaidlaw.mongo.webfs.DocumentNotFoundException
import ca.donlaidlaw.mongo.webfs.ValidationException
import ca.donlaidlaw.mongo.webfs.service.FilesystemService
import com.mongodb.gridfs.GridFSFile
import grails.converters.JSON
import grails.converters.XML

import javax.activation.MimeType
import org.springframework.http.HttpStatus

class FilesystemController {

    FilesystemService fs

    // TODO - use document metadata constraints
    String[] requiredFields = ["tenant", "name"]

    def get() {
        if (!params.tenant) {
            response.sendError HttpStatus.FORBIDDEN.value()
            return
        }

        if (params.id) {
            retrieveDocument()
        } else {
            searchDocuments()
        }
    }

    void retrieveDocument() {
        def document = fs.getDocument(params.tenant, params.id)
        if (document == null) {
            response.sendError HttpStatus.NOT_FOUND.value()
            return
        }
        response.setHeader("Content-disposition", "attachment; filename=${document.filename}")
        response.contentType = document.metadata.contentType
        copyMetadataToResponse(document.metadata)

        response << document.content
        response.flushBuffer()
    }

    private void searchDocuments() {
        // TODO - Need to implement this method in the service
        def documents = fs.findDocuments(request, params)
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
        validateInsert(params)
        params.put(FilesystemService.UPLOADED_BY, request.getUserPrincipal()?.getName())

        // TODO - read metadata from request

        GridFSFile file = null
        def contentType = request.contentType
        MimeType mimeType = new MimeType(contentType)
        if (mimeType.match("multipart/form-data")) {
            // This happens if the file is being uploaded with an upload form.
            def uploadFile = request.getFile('upload')
            // The next line can't happen because of validation, so do we want to be able to do this??
            if (!params.name) params.put('name', uploadFile.originalFileName)
            file = fs.insertFile(uploadFile.contentType, uploadFile.inputStream, params)
        } else {
            // Just take the post content as the file content.
            file = fs.insertFile(contentType, request.inputStream, params)
        }
        render file.id.toString()
    }

    /**
     * This is a PUT request. We only update the metadata, not the file content.
     * @return the file id if the file was found.
     */
    def update() {
        validateFields([FilesystemService.TENANT, FilesystemService.ID], params)
        params[FilesystemService.MODIFIED_BY] = request.getUserPrincipal()?.getName()
        GridFSFile file = fs.updateFile(params)
        if (file == null) {
            throw new DocumentNotFoundException("Document ID ${params[FilesystemService.ID]} not found.")
        }
        render(text: file.getId().toString(), contentType: "text/plain", encoding: "UTF8")
    }

    def delete() {
        validateFields([FilesystemService.TENANT, FilesystemService.ID], params)
        params[FilesystemService.MODIFIED_BY] = request.getUserPrincipal()?.getName()
        GridFSFile file = fs.deleteFile(params)
        if (file == null) {
            throw new DocumentNotFoundException("Document ID ${params[FilesystemService.ID]} not found.")
        }
        render(text: file.getId().toString(), contentType: "text/plain", encoding: "UTF8")
    }

    // helpers

    private def validateInsert(params) {
        validateFields(requiredFields, params)
    }

    private def validateFields(fields, params) {
        fields.each { name ->
            if (!params.containsKey(name)) {
                def nv = "field is"
                if (fields.size() > 1) nv = "fields are"
                throw new ValidationException("The following ${nv} required: $fields");
            }
        }
    }

    private def copyMetadataToResponse(DocumentMetadata metadata) {
        response.setHeader("WEBFS.ID", metadata.documentId);
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
    }

    private void addOptionalHeader(String headerName, Object value) {
        // TODO - check works good not only for lists but for scalars (well, should be)
        if (value) {
            value.each { response.addHeader(headerName, it) }
        }
    }
}
