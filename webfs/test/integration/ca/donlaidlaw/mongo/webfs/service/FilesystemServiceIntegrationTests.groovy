package ca.donlaidlaw.mongo.webfs.service

import com.mongodb.BasicDBObject
import com.mongodb.DB
import org.bson.types.ObjectId
import org.junit.Before
import org.junit.Test
import ca.donlaidlaw.mongo.webfs.*

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic

/**
 * Integration tests for {@code FilesystemService}
 */
class FilesystemServiceIntegrationTests {

    private static final String TENANT = "tenant"

    DB db

    FilesystemService filesystemService

    @Before
    void setUp() {
        // cleanup files
        db.getCollection("${filesystemService.bucket}.files").remove(new BasicDBObject())
        db.getCollection("${filesystemService.bucket}.chunks").remove(new BasicDBObject())
    }

    @Test
    void testInsertFile_DocumentIdReturned() {
        def metadata = createTestFileMetadata()
        def content = new ByteArrayInputStream("test content".bytes)

        def documentId = filesystemService.insertFile(metadata, content)

        assert documentId != null
    }

    @Test
    void testInsertFile_DocumentCreated() {
        def metadata = createTestFileMetadata()
        def content = new ByteArrayInputStream("test content".bytes)

        def documentId = filesystemService.insertFile(metadata, content)
        def retrievedMetadata = filesystemService.getFile(TENANT, documentId)

        assert retrievedMetadata != null
    }

    @Test(expected = ValidationException)
    void testInsertIncorrectFile() {
        def metadata = createTestFileMetadata()
        metadata.fileName = null

        def content = new ByteArrayInputStream("test content".bytes)
        filesystemService.insertFile(metadata, content)
    }

    @Test
    void testGetFileMetadata_MetadataRetrievedCorrectly() {
        def metadata = createTestFileMetadata()
        def content = new ByteArrayInputStream("test content".bytes)

        def documentId = filesystemService.insertFile(metadata, content)
        def retrievedMetadata = filesystemService.getFile(TENANT, documentId).metadata

        assert retrievedMetadata.fileName == metadata.fileName
        assert retrievedMetadata.fileSize == metadata.fileSize
        assert retrievedMetadata.note == metadata.note
        assert retrievedMetadata.modifiedBy == metadata.modifiedBy
        assert retrievedMetadata.uploadedBy == metadata.uploadedBy
        assert retrievedMetadata.classifier == metadata.classifier
        assert retrievedMetadata.tags == metadata.tags
        assert retrievedMetadata.references == metadata.references
        assert retrievedMetadata.md5Checksum != null
    }

    // --------------------------------- search

    @Test
    void testSearchFiles_ResultsPageCorrect() {
        def page = new Page(page: 2, perPage: 5)

        def conditions = new FileSearchConditions(tenant: TENANT)
        def results = filesystemService.searchFiles(conditions, page)

        assert results.page == page.page
        assert results.perPage == page.perPage
    }

    @Test
    void testGetRelatedFiles_ResultCorrect() {
        def content = new ByteArrayInputStream("test content".bytes)

        // page 1
        filesystemService.insertFile(createTestFileMetadata(), content)
        filesystemService.insertFile(createTestFileMetadata(), content)
        filesystemService.insertFile(createTestFileMetadata(), content)
        filesystemService.insertFile(createTestFileMetadata(), content)
        filesystemService.insertFile(createTestFileMetadata(), content)

        // page 2
        filesystemService.insertFile(createTestFileMetadata(), content)
        filesystemService.insertFile(createTestFileMetadata(), content)

        def page = new Page(page: 2, perPage: 5)

        def conditions = new FileSearchConditions(tenant: TENANT)
        def results = filesystemService.searchFiles(conditions, page)

        assert results.list.size() == 2
    }

    @Test
    void testGetFilesWithNoReferences_ResultCorrect() {
        def content = new ByteArrayInputStream("test content".bytes)

        // page 1
        filesystemService.insertFile(createTestFileMetadata(), content)
        def withoutReferenceId = filesystemService.insertFile(createTestFileMetadataWithoutReferences(), content)

        def page = new Page(page: 1, perPage: 5)
        def conditions = new FileSearchConditions(tenant: TENANT)
        conditions.emptyReference = true
        def results = filesystemService.searchFiles(conditions, page)

        assert results.list.size() == 1
        assert results.list.first().fileId == withoutReferenceId
    }

    // --------------------------------- update metadata

    @Test
    void testUpdateMetadata_Correct() {
        // create
        def metadata = createTestFileMetadata()
        def content = new ByteArrayInputStream("test content".bytes)
        filesystemService.insertFile(metadata, content)

        // update
        metadata.tags = ["tag1", "tag2", "tag3"]
        filesystemService.updateFile(metadata)

        // check
        def retrievedDocument = filesystemService.getFile(TENANT, metadata.fileId).metadata

        assert retrievedDocument.tags.size() == metadata.tags.size()
        assert retrievedDocument.tags.containsAll(metadata.tags)
    }

    @Test(expected = FileNotFoundException)
    void testUpdateMetadataOfAbsentDocument_throwException() {
        def metadataWithAbsentId = createTestFileMetadata()
        metadataWithAbsentId.fileId = ObjectId.newInstance().toString()

        filesystemService.updateFile(metadataWithAbsentId)
    }

    // --------------------------------- utils

    private FileMetadata createTestFileMetadata() {
        new FileMetadata(
                tenant: TENANT,
                fileName: randomAlphabetic(10) + '.pdf',
                fileSize: 12,
                classifier: 'test type',
                md5Checksum: null,
                owner: 'testUser',
                tags: ["tag1", "tag2"] as Set,
                references: ["ref1", "ref2"] as Set,
                note: "test version note"
        )
    }

    private FileMetadata createTestFileMetadataWithoutReferences() {
        new FileMetadata(
                tenant: TENANT,
                fileName: randomAlphabetic(10) + '.pdf',
                fileSize: 12,
                classifier: 'test type',
                md5Checksum: null,
                owner: 'testUser',
                tags: ["tag1", "tag2"] as Set,
                references: [] as Set,
                note: "test version note"
        )
    }


}
