package ca.donlaidlaw.mongo.webfs.controller

import grails.converters.JSON
import org.bson.types.ObjectId
import org.junit.Test

import static com.jayway.restassured.RestAssured.given

/**
 * Integration tests for {@code DocumentRESTController}.
 *
 * @author Ruslan Khmelyuk
 */
class FilesystemControllerFunctionalTests {

    String testTenantUri = "http://localhost:8080/webfs/v1.0/test"
    String otherTenantUri = "http://localhost:8080/webfs/v1.0/other"

    // add file

    @Test
    void whenInsertIncorrectFile_then409Returned() {
        def response = given().contentType("application/octet-stream").post(testTenantUri)
        assert response.statusCode == 409
    }

    @Test
    void whenInsertIncorrectFile_theCorrectErrorReturned() {
        def response = given().post(testTenantUri)
        def content = JSON.parse(response.asString())
        assert content.error.size() == 1
    }

    @Test
    void whenInsertFile_then200Returned() {
        def response = givenWithParams().body("test content").post(testTenantUri)

        assert response.statusCode == 200
    }

    @Test
    void whenInsertFile_thenMetadataReturned() {
        def response = givenWithParams().body("test content").post(testTenantUri)

        assert response.asString() != null
    }

    @Test
    void whenInsertFileThroughUpload_then200Returned() {
        def response = givenWithParams().multiPart("upload", "test_file.pdf", "test content".bytes).post(testTenantUri)

        assert response.statusCode == 200
    }

    @Test
    void whenInsertFileThroughUpload_thenMetadataReturned() {
        def response = givenWithParams().multiPart("upload", "test_file.pdf", "test content".bytes).post(testTenantUri)

        assert response.asString() != null
    }

    @Test
    void whenInsertFileWithIncorrectMD5_then409Returned() {
        def response = given().queryParams([
                name: 'test.pdf',
                tags: 'tag1,tag2,tag3',
                notes: 'test notes',
                type: 'test type',
                md5: 'incorrect md5'
        ]).body("test content").post(testTenantUri)

        assert response.statusCode == 409
    }

    @Test
    void whenInsertFileWithIncorrectMD5_thenCorrectErrorReturned() {
        def response = given().queryParams([
                name: 'test.pdf',
                tags: 'tag1,tag2,tag3',
                notes: 'test notes',
                type: 'test type',
                md5: 'incorrect md5'
        ]).body("test content").post(testTenantUri)

        def content = JSON.parse(response.asString())

        assert content.error == "invalid.md5.checksum"
    }

    // get file

    @Test
    void givenFile_whenGetById_then200Returned() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.statusCode == 200
    }

    @Test
    void givenFile_whenGetById_thenCorrectContentReturned() {
        final def testContent = "some test content"
        def addResponse = givenWithParams().body(testContent).post(testTenantUri)
        def fileId = addResponse.asString()

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.asString() == testContent
    }

    @Test
    void givenFile_whenGetById_thenCorrectMetadataReturned() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.header('WEBFS.ID') == fileId
        assert getResponse.header('WEBFS.NAME') == "test.pdf"
        assert getResponse.header('WEBFS.CLASSIFIER') == "test classifier"
        assert getResponse.header('WEBFS.NOTE') == "test note"
    }

    @Test
    void whenGetByUnknownId_then404Returned() {
        def fileId = ObjectId.newInstance().toString()

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.statusCode == 404
    }

    @Test
    void whenGetByOthersFileById_then403Returned() {
        def addResponse = givenWithParams().body("test content").post(otherTenantUri)
        def fileId = addResponse.asString()

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.statusCode == 403
    }

    // update

    // TODO - test when modifiedBy is null?

    //helpers

    private static def givenWithParams() {
        return given()
                .queryParam('name', 'test.pdf')
                .queryParam('tags', 'tag1')
                .queryParam('tags', 'tag2')
                .queryParam('tags', 'tag3')
                .queryParam('classifier', 'test classifier')
                .queryParam('note', 'test note')
                .queryParam('references', 'ref1')
                .queryParam('references', 'ref2')
                .queryParam('references', 'ref3')
    }

}
