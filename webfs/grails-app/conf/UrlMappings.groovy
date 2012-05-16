class UrlMappings {

	static mappings = {
		"/v1.0/$tenant/$id?"(controller: "filesystem", parseRequest: false) {
			action = [GET: "get", PUT: "update", DELETE: "delete", POST: "insert"]
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
