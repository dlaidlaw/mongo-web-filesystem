class UrlMappings {

	static mappings = {
		"/v1.0/$tenant/?"(controller: "filesystem", parseRequest: false) {
			action = [GET: "get", POST: "insert"]
		}

        "/v1.0/$tenant/$id/?"(controller: "filesystem", parseRequest: false) {
            action = [GET: "get", PUT: "update", DELETE: "delete"]
        }

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
