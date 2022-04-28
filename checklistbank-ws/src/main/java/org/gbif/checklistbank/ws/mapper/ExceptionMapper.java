package org.gbif.checklistbank.ws.mapper;

import org.gbif.ws.NotFoundException;
import org.gbif.ws.WebApplicationException;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class ExceptionMapper {

  @Autowired private ErrorAttributes errorAttributes;

  @SuppressWarnings("rawtypes")
  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity<Object> handleWebApplicationException(
      WebRequest request, NotFoundException e) {
    return buildResponse(request, e.getStatus(), e.getMessage());
  }

  private ResponseEntity<Object> buildResponse(WebRequest request, int status, String message) {
    HttpStatus httpStatus = HttpStatus.resolve(status);
    if (httpStatus == null) {
      throw new IllegalArgumentException("Invalid http status: " + status);
    }

    Map<String, Object> body =
        errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    body.put("status", httpStatus.value());
    Optional.ofNullable(message).ifPresent(m -> body.put("message", m));
    body.put("error", "");

    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
  }
}
