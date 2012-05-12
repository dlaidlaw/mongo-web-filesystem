package ca.donlaidlaw.mongo.webfs.service

import javax.servlet.http.HttpServletRequest;

import com.mongodb.BasicDBObject
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.gridfs.GridFSInputFile;

class FilesystemService {
	DB db
	String bucket = "fs"
	String root = ""
	
    def insertFile(HttpServletRequest request, Map<String, Object> params, long version) {
		GridFS gridFS = new GridFS(db, bucket)
		String filename = makeGridFSFilename(params.name)
		GridFSInputFile gridFile = gridFS.createFile(request.inputStream, filename, false)
		gridFile.setContentType(request.contentType)
		DBObject metadata = gridFile.getMetaData()
		if (metadata == null) {
			metadata = new BasicDBObject()
			gridFile.setMetaData(metadata)
		}
		gridFile.put('tenant', params.tenant)
		metadata.put('createdBy', params.createdBy)
		metadata.put('class', params.class)
		metadata.put('tags', params.list('tags'))
		metadata.put('version', version)
		
		gridFile.save();
		return gridFile;
    }
	
	String makeGridFSFilename(String name) {
		String filename = name
		if (!filename.startsWith("/")) filename = root + "/" + filename
		else filename = root + filename
		return filename
	}
}
