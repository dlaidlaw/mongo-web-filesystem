class UrlMappings {

	static mappings = {
		"/v1.0/$tenant/$id?"(controller: "filesystem", parseRequest: false) {
			action = [GET: "show", PUT: "update", DELETE: "delete", POST: "save"]
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
