package ca.donlaidlaw.mongo.webfs.search

/**
 * The result of search.
 */
class Result<T> {

    int page
    int perPage

    List<T> list = Collections.emptyList()
}
