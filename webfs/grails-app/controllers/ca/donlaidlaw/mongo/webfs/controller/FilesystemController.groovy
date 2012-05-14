package ca.donlaidlaw.mongo.webfs.controller

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
	
	def update() {
		response.sendError response.SC_NOT_IMPLEMENTED, "Update not implemented."
	}
	
	def delete() {
		response.sendError response.SC_NOT_IMPLEMENTED, "Delete not implemented."
	}
	
	/**
	 * Will insert a file into the file store.
	 * @return the id of the file as a string.
	 */
	def save() {
		validateInsert()
		GridFSFile file = fs.insertFile(request, params)
		render file.id.toString()
	}
	
	def validateInsert(params) {
		validateRequiredFields(params)
	}
	
	def validateRequiredFields(params) {
		requiredFields.each { name ->
			if (!params[name]) {
				throw new ValidationException("The following fields are required: $requiredFields");
			}
		}
	}
}
