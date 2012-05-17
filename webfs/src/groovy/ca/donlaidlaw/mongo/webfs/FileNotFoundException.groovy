package ca.donlaidlaw.mongo.webfs

class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);

    }

	public FileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}