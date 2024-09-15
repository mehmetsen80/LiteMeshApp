package org.lite.mesh.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = SpelEvaluationException.class)
    public ModelAndView handleSpelEvaluationException(SpelEvaluationException ex, Model model) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500); // Assuming HTTP status 500 for server errors
        mav.addObject("error", "SpEL Evaluation Error");
        mav.addObject("message", ex.getMessage());
        mav.addObject("timestamp", new java.util.Date());
        mav.setViewName("error"); // Points to your error.html template
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralError(HttpServletRequest request,
                                     HttpServletResponse response, Exception ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500); // Assuming HTTP status 500 for server errors
        mav.addObject("error", "Internal Server Error");
        mav.addObject("message", ex.getMessage());
        mav.addObject("timestamp", new java.util.Date());
        mav.addObject("exception", ex);
        mav.addObject("url", request.getRequestURL());
        mav.setViewName("error"); // Points to your error.html template
        return mav;
    }

    // You can add more handlers for other specific exceptions here if needed.
}
