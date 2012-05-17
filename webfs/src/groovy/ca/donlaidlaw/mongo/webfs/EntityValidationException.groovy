package ca.donlaidlaw.mongo.webfs

/**
 * Failed to validate entity or model object.
 */
class EntityValidationException extends ValidationException {

    EntityValidationException() {
        super("faailed to validate entity")
    }
}
