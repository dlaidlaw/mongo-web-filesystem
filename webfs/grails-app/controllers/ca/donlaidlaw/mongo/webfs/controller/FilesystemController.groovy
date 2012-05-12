package ca.donlaidlaw.mongo.webfs.controller

import ca.donlaidlaw.mongo.webfs.ValidationException
import ca.donlaidlaw.mongo.webfs.service.FilesystemService
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSFile;

class FilesystemController {
	FilesystemService fs
	
	String[] requiredFields = ["tenant", "name"]

    def show() {
		response.sendError response.SC_NOT_IMPLEMENTED, "Get not implemented."
	}
	
	def update() {
		response.sendError response.SC_NOT_IMPLEMENTED, "Update not implemented."
	}
	
	def delete() {
		response.sendError response.SC_NOT_IMPLEMENTED, "Delete not implemented."
	}
	
	def save() {
		validateInsert()
		GridFSFile file = fs.insertFile(request, params, 1L)
		render file.id
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
