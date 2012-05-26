package ca.donlaidlaw.mongo.webfs.search

/**
 * Used to retrieve a page of documents or other entities on search.
 */
class Page {

    int page
    int perPage

    Page() {
        // default c-tor
    }

    Page(int page, int perPage) {
        this.page = page
        this.perPage = perPage
    }
}
