package ca.donlaidlaw.mongo.webfs.controller

import ca.donlaidlaw.mongo.webfs.ValidationException
import grails.converters.JSON

import javax.servlet.http.HttpServletResponse

/**
 * A base class for REST controllers.
 *
 * @author Ruslan Khmelyuk
 */
abstract class AbstractRESTController {

    static final int SC_SUCCESS = HttpServletResponse.SC_OK;
    static final int SC_CONFLICT = HttpServletResponse.SC_CONFLICT;
    static final int SC_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;
    static final int SC_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;

    protected void renderErrors(obj) {
        def errors = obj.errors.allErrors.collect { g.message(error: it) }
        render(contentType: "text/json", status: SC_CONFLICT) {
            error {
                for (err in errors) {
                    msg(error: err)
                }
            }
        }
    }

    protected void renderError(ValidationException ex) {
        render(text: [error: ex.message] as JSON, status: SC_CONFLICT)
    }
}
