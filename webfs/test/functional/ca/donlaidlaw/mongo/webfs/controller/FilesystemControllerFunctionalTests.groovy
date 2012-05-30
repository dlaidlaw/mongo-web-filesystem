package ca.donlaidlaw.mongo.webfs.controller

import grails.converters.JSON
import org.bson.types.ObjectId
import org.junit.Test

import static com.jayway.restassured.RestAssured.given
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic

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
    void whenInsertFile_thenIdReturned() {
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

    // search file

    @Test
    void givenFile_whenSearchByFilename_then200Returned() {
        givenWithParams(name: "test.pdf").body("test content").post(testTenantUri)
        givenWithParams(name: "othr.pdf").body("test content").post(testTenantUri)

        def getResponse = given().queryParam("name", "test.pdf").get(testTenantUri)

        assert getResponse.statusCode == 200
    }

    @Test
    void givenFile_whenSearchByFilename_thenFound() {
        def filename = randomAlphabetic(20);
        def file1Id = givenWithParams(name: filename).body("test content").post(testTenantUri).asString()
        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()

        def getResponse = given().queryParam("name", filename).get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFile_whenSearchByClassifier_thenFound() {
        def classifier = randomAlphabetic(20);
        def file1Id = givenWithParams(classifier: classifier).body("test content").post(testTenantUri).asString()
        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()

        def getResponse = given().queryParam("classifier", classifier).get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFile_whenSearchByTag_thenFound() {
        def tag = randomAlphabetic(20);
        def file1Id = givenWithParams(tag1: tag).body("test content").post(testTenantUri).asString()
        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()

        def getResponse = given().queryParam("tag", tag).get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFile_whenSearchByReference_thenFound() {
        def ref = randomAlphabetic(20);
        def file1Id = givenWithParams(ref2: ref).body("test content").post(testTenantUri).asString()
        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()

        def getResponse = given().queryParam("reference", ref).get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFile_whenSearchByOwner_thenFound() {
        def owner = randomAlphabetic(20);
        def file1Id = givenWithParams(owner: owner).body("test content").post(testTenantUri).asString()
        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()

        def getResponse = given().queryParam("owner", owner).get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFile_whenSearchByMultipleConditions_thenFound() {
        def tag = randomAlphabetic(20);
        def name = randomAlphabetic(20);
        def owner = randomAlphabetic(20);

        def file2Id = givenWithParams().body("test content").post(testTenantUri).asString()
        def file1Id = givenWithParams(name: name, owner: owner, tag2: tag).body("test content").post(testTenantUri).asString()

        def getResponse = given()
                .queryParam("name", name)
                .queryParam("owner", owner)
                .queryParam("tag", tag)
                .get(testTenantUri)

        def response = getResponse.asString()
        assert response.contains(file1Id)
        assert !response.contains(file2Id)
    }

    @Test
    void givenFileOfOtherUser_whenSearchByFilename_thenNotFound() {
        def filename = randomAlphabetic(20);
        def fileId = givenWithParams(name: filename).body("test content").post(otherTenantUri).asString()

        def getResponse = given().queryParam("name", filename).get(testTenantUri)

        assert !getResponse.asString().contains(fileId)
    }

    // update

    @Test
    void givenFile_whenUpdate_then200Returned() {
        def addResponse = givenWithParams().body("test content").post(testTenantUri)
        def fileId = addResponse.asString()

        def updateResponse = givenWithUpdateParams().put(testTenantUri + "/$fileId")

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
        assert getResponse.header('WEBFS.OWNER') == "test_owner"

        assert getResponse.headers.getValues('WEBFS.TAGS').size() == 2
        assert getResponse.headers.getValues('WEBFS.TAGS').containsAll(["updated_tag1", "updated_tag2"])

        assert getResponse.headers.getValues('WEBFS.REFERENCES').size() == 4
        assert getResponse.headers.getValues('WEBFS.REFERENCES').containsAll([
                "updated_ref1", "updated_ref2", "updated_ref3", "updated_ref4"])
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

    private static def givenWithParams(def params = [:]) {
        return given()
                .queryParam('name', params.name ?: 'test.pdf')
                .queryParam('tags', params.tag1 ?: 'tag1')
                .queryParam('tags', params.tag2 ?: 'tag2')
                .queryParam('tags', params.tag3 ?: 'tag3')
                .queryParam('classifier', params.classifier ?: 'test classifier')
                .queryParam('note', params.note ?: 'test note')
                .queryParam('references', params.ref1 ?: 'ref1')
                .queryParam('references', params.ref2 ?: 'ref2')
                .queryParam('references', params.ref3 ?: 'ref3')
                .queryParam('owner', params.owner ?: 'test_owner')
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
