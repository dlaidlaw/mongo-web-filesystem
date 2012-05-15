package ca.donlaidlaw.mongo.webfs.controller

import javax.activation.MimeType;

import grails.converters.JSON;
import grails.converters.XML;
import ca.donlaidlaw.mongo.webfs.DocumentNotFoundException;
import ca.donlaidlaw.mongo.webfs.ValidationException;
import ca.donlaidlaw.mongo.webfs.service.FilesystemService;
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSFile;

class FilesystemController {
	FilesystemService fs
	
	String[] requiredFields = ["tenant", "name"]

    def show() {
		if (!params.tenant) {
			throw new ValidationException("The tenant is required.")
		}
		if (!params.id) {
			// Need to implement this method in the service
			def documents = fs.findDocuments(request, params)
			withFormat {
				xml { render documents as XML }
				json { render documents as JSON }
			}
			render documents as JSON
		} else {
			def document = fs.getDocument(params.tenant, params.id)
			if (document == null) {
				throw new DocumentNotFoundException("Document ID ${params.id} not found.")
			}
			response.setHeader("Content-disposition", "attachment; filename=${document.filename}")
			response.setContentType(document.getContentType())
			fs.copyMetadataToResponse(document, response)
			document.writeTo(response.outputStream)
			response.outputStream.flush()
		}
		
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
		render (text: file.getId().toString(), contentType: "text/plain", encoding: "UTF8")
	}
	
	def delete() {
		validateFields([FilesystemService.TENANT, FilesystemService.ID], params)
		params[FilesystemService.MODIFIED_BY] = request.getUserPrincipal()?.getName()
		GridFSFile file = fs.deleteFile(params)
		if (file == null) {
			throw new DocumentNotFoundException("Document ID ${params[FilesystemService.ID]} not found.")
		}
		render (text: file.getId().toString(), contentType: "text/plain", encoding: "UTF8")
	}
	
	/**
	 * Will insert a file into the file store.
	 * @return the id of the file as a string.
	 */
	def save() {
		validateInsert(params)
		params.put(FilesystemService.UPLOADED_BY, request.getUserPrincipal()?.getName())
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
			file = fs.insertFile(contentType, request.inputStream, uploadedBy, params)
		}
		render file.id.toString()
	}
	
	def validateInsert(params) {
		validateFields(requiredFields, params)
	}
	
	def validateFields(fields, params) {
		fields.each { name ->
			if (!params.containsKey(name)) {
				def nv = "field is"
				if (fields.size() > 1) nv = "fields are"
				throw new ValidationException("The following ${nv} required: $fields");
			}
		}
	}
}
