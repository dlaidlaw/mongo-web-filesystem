package ca.donlaidlaw.mongo.webfs

/**
 * We check if MD5 checksum is correct, if it's not then this exception should be thrown.
 */
class InvalidMD5ChecksumException extends ValidationException {

    InvalidMD5ChecksumException() {
        super("invalid.md5.checksum")
    }
}
