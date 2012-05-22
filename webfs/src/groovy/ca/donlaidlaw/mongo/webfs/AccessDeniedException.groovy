package ca.donlaidlaw.mongo.webfs

/**
 * Error to access when not allowed.
 *
 * @author Ruslan Khmelyuk
 */
class AccessDeniedException extends RuntimeException {

    AccessDeniedException(String msg) {
        super(msg);
    }
}
