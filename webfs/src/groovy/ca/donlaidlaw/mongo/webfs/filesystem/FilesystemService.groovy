package ca.donlaidlaw.mongo.webfs.filesystem

import ca.donlaidlaw.mongo.webfs.*
import ca.donlaidlaw.mongo.webfs.search.Page
import ca.donlaidlaw.mongo.webfs.search.FileSearchConditions
import ca.donlaidlaw.mongo.webfs.search.Result

/**
 * An interface for FS service.
 *
 * @author Ruslan Khmelyuk
 */
public interface FilesystemService {

    /**
     * Inserts new file to the storage.
     * @param metadata a file metadata.
     * @param content an input stream with new file content.
     * @return the database id of just added file.
     */
    String insertFile(FileMetadata metadata, InputStream content)

    /**
     * Updates the file metadata only.
     * This method doesn't update a file content or version, also it doesn't update creator and create date information.
     *
     * @param metadata the file metadata.
     * @return the file id
     */
    void updateFile(FileMetadata metadata)

    /**
     * Delete file by id.
     *
     * @param fileId the file id.
     * @param tenant the tenant.
     */
    void deleteFile(String fileId, String tenant)

    /**
     * Find the file by id.
     *
     * @param tenant the tenant.
     * @param id the file id.
     * @return the found file information.
     * @throws FileNotFoundException if specified file was not found
     * @throws FileAccessDeniedException if user has no access to the specified file
     */
    WebFSFile getFile(String tenant, String id)

    /**
     * Find file metadata by id.
     *
     * @param tenant the tenant.
     * @param id the file id.
     * @return the found file metadata.
     * @throws FileNotFoundException if specified file was not found
     * @throws FileAccessDeniedException if user has no access to the specified file
     */
    FileMetadata getFileMetadata(String tenant, String id)

    /**
     * Search files by specified conditions.
     *
     * @param conditions the search conditions.
     * @param page the information about page that need to be retrieved.
     * @return the list of found files metadata.
     * @throws AccessDeniedException when tenant is not specified.
     */
    Result<FileMetadata> searchFiles(FileSearchConditions conditions, Page page)

}