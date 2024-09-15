package org.lite.mesh.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        Throwable error = getError(webRequest);
        if (error instanceof SpelEvaluationException) {
            errorAttributes.put("message", "A SpEL evaluation error occurred: " + error.getMessage());
            errorAttributes.put("error", "SpEL Evaluation Error");
        }

        return errorAttributes;
    }
}
