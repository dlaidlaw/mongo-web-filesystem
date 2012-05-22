package ca.donlaidlaw.mongo.webfs

/**
 * The result of search.
 */
class Result<T> {

    int page
    int perPage

    List<T> list = Collections.emptyList()
}
