package ca.donlaidlaw.mongo.webfs.controller

import grails.converters.JSON
import org.bson.types.ObjectId
import org.junit.Test

import static com.jayway.restassured.RestAssured.given
import org.junit.Ignore

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
        assert getResponse.header('WEBFS.TENANT') == "test"
        assert getResponse.header('WEBFS.NAME') == "test.pdf"
        assert getResponse.header('WEBFS.CLASSIFIER') == "test classifier"
        assert getResponse.header('WEBFS.NOTE') == "test note"
        assert getResponse.header('WEBFS.SIZE') == "12"
        assert getResponse.header('WEBFS.MD5') != null
        assert getResponse.header('WEBFS.UPLOAD_DATE') != null
        assert getResponse.header('WEBFS.OWNER') == "test_owner"

        assert getResponse.headers.getValues('WEBFS.TAGS').size() == 3
        assert getResponse.headers.getValues('WEBFS.TAGS').containsAll(["tag1", "tag2", "tag3"])

        assert getResponse.headers.getValues('WEBFS.REFERENCES').size() == 3
        assert getResponse.headers.getValues('WEBFS.REFERENCES').containsAll(["ref1", "ref2", "ref3"])
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

    @Test
    void givenFile_whenUpdate_then200Returned() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def updateResponse = givenWithUpdateParams().auth().preemptive().basic("test_user", "test_password").put(testTenantUri + "/$fileId")

        assert updateResponse.statusCode == 200
    }

    @Test
    void givenFile_whenUpdate_thenMetadataUpdated() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        givenWithUpdateParams().put(testTenantUri + "/$fileId")

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.header('WEBFS.ID') == fileId
        assert getResponse.header('WEBFS.NAME') == "updated_test.pdf"
        assert getResponse.header('WEBFS.TENANT') == "test"
        assert getResponse.header('WEBFS.CLASSIFIER') == "updated test classifier"
        assert getResponse.header('WEBFS.NOTE') == "updated test note"
        assert getResponse.header('WEBFS.SIZE') == "12"
        assert getResponse.header('WEBFS.MD5') != null
        assert getResponse.header('WEBFS.UPLOAD_DATE') != null
        assert getResponse.header('WEBFS.OWNER') == "updated_test_owner"

        assert getResponse.headers.getValues('WEBFS.TAGS').size() == 2
        assert getResponse.headers.getValues('WEBFS.TAGS').containsAll(["updated_tag1", "updated_tag2"])

        assert getResponse.headers.getValues('WEBFS.REFERENCES').size() == 4
        assert getResponse.headers.getValues('WEBFS.REFERENCES').containsAll([
                "updated_ref1", "updated_ref2", "updated_ref3", "updated_ref4"])
    }

    @Test
    @Ignore("not ready for use yet")
    void givenFile_whenUpdateWithNewContent_then200() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def updateResponse = givenWithUpdateParams().body("updated test content").put(testTenantUri + "/$fileId")

        assert updateResponse.statusCode == 200
    }

    // delete

    @Test
    void givenFile_whenDelete_then200Returned() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def deleteResponse = given().delete(testTenantUri + "/$fileId")

        assert deleteResponse.statusCode == 200
    }

    @Test
    void givenFile_whenDelete_thenFileIsDeleted() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        given().delete(testTenantUri + "/$fileId")

        def getResponse = given().get(testTenantUri + "/$fileId")

        assert getResponse.statusCode == 404
    }

    @Test
    void whenDeleteFileWithUnknownId_then404Returned() {
        def fileId = ObjectId.newInstance().toString()

        def deleteResponse = given().delete(testTenantUri + "/$fileId")

        assert deleteResponse.statusCode == 404
    }

    @Test
    void whenDeleteOthersFileById_then403Returned() {
        def addResponse = givenWithParams().body("test content").post(otherTenantUri)
        def fileId = addResponse.asString()

        def deleteResponse = given().delete(testTenantUri + "/$fileId")

        assert deleteResponse.statusCode == 403
    }

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
                .queryParam('owner', 'test_owner')
    }

    private static def givenWithUpdateParams() {
        return given()
                .queryParam('name', 'updated_test.pdf')
                .queryParam('tags', 'updated_tag1')
                .queryParam('tags', 'updated_tag2')
                .queryParam('classifier', 'updated test classifier')
                .queryParam('note', 'updated test note')
                .queryParam('references', 'updated_ref1')
                .queryParam('references', 'updated_ref2')
                .queryParam('references', 'updated_ref3')
                .queryParam('references', 'updated_ref4')
                .queryParam('owner', 'updated_test_owner')
    }

}
