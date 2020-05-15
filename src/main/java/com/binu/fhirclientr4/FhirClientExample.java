package com.binu.fhirclientr4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

import com.binu.fhirclientr4.util.BundleFetcher;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic FHIR client example using HAPI FHIR.
 */
public final class FhirClientExample {
    private static final Logger LOG = LoggerFactory.getLogger(FhirClientExample.class);

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    static {
        /*
          Could disable server validation (don't pull the server's metadata first) to allow for interaction with
          FHIR servers that don't have a conformance statement:
        */
        // FHIR_CONTEXT.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

        /*
          HAPI FHIR allows you to configure how lenient/strict the parser should be.
          By default, any invalid values cause a DataFormatException to be thrown.
          It could be configured to be even more lenient and instead simply log a warning if there are parsing errors
          involving invalid values, but that could technically lead to a loss of data, so take that into consideration:
         */
        // FHIR_CONTEXT.setParserErrorHandler(new LenientErrorHandler().setErrorOnInvalidValue(false));
    }
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred.";

    // Parameter definitions to be used as part of the search criteria
    private static final TokenClientParam ID_PARAM = new TokenClientParam(Patient.SP_RES_ID);
    private static final TokenClientParam IDENTIFIER_PARAM = new TokenClientParam(Patient.SP_IDENTIFIER);
    private static final StringClientParam FAMILY_PARAM = new StringClientParam(Patient.SP_FAMILY);
    private static final StringClientParam GIVEN_PARAM = new StringClientParam(Patient.SP_GIVEN);
    private static final DateClientParam BIRTHDATE_PARAM = new DateClientParam(Patient.SP_BIRTHDATE);
    private static final TokenClientParam GENDER_PARAM = new TokenClientParam(Patient.SP_GENDER);

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private FhirClientExample () {}

    /**
     * The main method to run the example code.
     * @param args command line arguments, which should be the base URL of the FHIR server
     */
    public static void main (String[] args) {
        // Getting the base URL from the command line argument.
        if (args.length == 0) {
            throw new IllegalStateException("The base URL for the FHIR server must be specified as an argument. " +
                    "For example: https://hapi.fhir.org/baseDstu2");
        }
        String baseUrl = args[0];
        LOG.debug("Base URL is {}", baseUrl);
        IGenericClient client = FHIR_CONTEXT.newRestfulGenericClient(baseUrl);


        /*
          HAPI FHIR allows "interceptors" to be added to do special behaviors to the HTTP request right before it is
          sent and to do special behaviors to the HTTP response before HAPI FHIR starts processing the response.
          HAPI FHIR includes a few client interceptors out of the box but custom interceptors can easily be created.
          More info about how to use client interceptors can be found here:
            http://hapifhir.io/doc_rest_client_interceptor.html
          More info about HAPI FHIR's included client interceptors can be found here:
            http://hapifhir.io/apidocs/ca/uhn/fhir/rest/client/interceptor/package-summary.html
         */

        /*
          HAPI FHIR's AdditionalRequestHeadersInterceptor which can be used to add some arbitrary additional headers
          to the request, if needed.
         */
        AdditionalRequestHeadersInterceptor additionalHttpHeadersInterceptor =
                new AdditionalRequestHeadersInterceptor();
        additionalHttpHeadersInterceptor.addHeaderValue("Foo-Header-1", "fooHeaderValue1");
        additionalHttpHeadersInterceptor
                .addAllHeaderValues("Foo-Header-2", Stream.of("fooHeaderValue2a", "fooHeaderValue2b")
                        .collect(Collectors.toList()));


        /*
          HAPI FHIR's BasicAuthInterceptor which can be used to add an HTTP Basic authorization header with the
          specified username/password, if needed.
         */
        BasicAuthInterceptor basicAuthInterceptor =
                new BasicAuthInterceptor("myArbitraryUsername", "myArbitraryPassword");


        /*
          HAPI FHIR's LoggingInterceptor, for example, can log a variety of different request and response information.
          Note that the LoggingInterceptor logs at the INFO level, so only register this logger if you are *sure*
          you want to log this information.
          Maybe a special logger should be created for logging this data in a real application so that it can be
          specifically enabled/disabled independently.
        */
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        // Setting the logger to this class's logger so that the logging can be controlled by us.
        loggingInterceptor.setLogger(LOG);
        loggingInterceptor.setLogRequestSummary(true);
        loggingInterceptor.setLogRequestHeaders(true);
        loggingInterceptor.setLogRequestBody(false);
        loggingInterceptor.setLogResponseSummary(true);
        loggingInterceptor.setLogResponseHeaders(false);
        loggingInterceptor.setLogResponseBody(true);


        // Interceptors then have to be registered to the client for them to be used.
        // Note: Interceptors are executed in the order that they are registered in.
        //client.registerInterceptor(additionalHttpHeadersInterceptor);
        // Commenting out for now so that no authentication info is sent to public test FHIR servers
        //client.registerInterceptor(basicAuthInterceptor);

        // Commenting out for now since it can create very verbose output
        //client.registerInterceptor(loggingInterceptor);

        try {
            /*
              Example code showing usages of a handful of specific search parameters.
              Just showing how to define various search parameters.
              GET [base]/Patient?_id=1018&identifier=DRGHAFLGFDLMRN&family=Reynolds&given=Dennis&birthdate=1976-04-13&gender=male
            */
            Bundle verySpecificSearchResultsBundle = client
                    .search()
                    .forResource(Patient.class)
                    .where(ID_PARAM.exactly().code("010"))
                    //.and(IDENTIFIER_PARAM.exactly().code("DRGHAFLGFDLMRN"))
                    // Could also do FAMILY_PARAM.contains() to use the :contains search modifier
                    .and(FAMILY_PARAM.matches().value("Bond"))
                    .and(GIVEN_PARAM.matches().value("James"))
                    //.and(BIRTHDATE_PARAM.exactly().day("1976-04-13"))
                    //.and(GENDER_PARAM.exactly().code(AdministrativeGender.MALE.toCode()))
                    /*
                      Used to explicitly specify that you want a JSON response if you want to enforce that.
                     */
                    //.encodedJson()
                    .returnBundle(Bundle.class)
                    .execute();
            LOG.info("{} patient(s) matched in the very specific search and {} patient(s) are in this bundle.",
                    verySpecificSearchResultsBundle.getTotal(), verySpecificSearchResultsBundle.getEntry().size());
            for (BundleEntryComponent bec : verySpecificSearchResultsBundle.getEntry()) {
                Patient p = (Patient) bec.getResource();
                LOG.info("ID of found patient in the very specific search is {}", p.getIdElement().getIdPart());
            }

            // Example searching for patients with a specific family name.
            // GET [base]/Patient?family=Reynolds
            Bundle firstBundle = client
                    .search()
                    .forResource(Patient.class)
                    .where(FAMILY_PARAM.matchesExactly().value("Melonseed"))
                    .returnBundle(Bundle.class)
                    .execute();
            /*
              From that search, there could be pages of results, so let's gather up all of the patients
              from all of the pages into one bundle
             */
            Bundle aggregateResultsBundle = new BundleFetcher(client, firstBundle).fetchAll();

            LOG.info("In the end the search matched {} patient(s) and {} patient(s) are in this aggregate bundle.",
                    aggregateResultsBundle.getTotal(), aggregateResultsBundle.getEntry().size());
            // Log the IDs of the patients that were returned.
            for (BundleEntryComponent bec : aggregateResultsBundle.getEntry()) {
                Patient p = (Patient) bec.getResource();
                LOG.info("ID of found patient is {}", p.getIdElement().getIdPart());
            }
        } catch (BaseServerResponseException bsr) {
            /*
              The server can cause exceptions.
              See http://hapifhir.io/apidocs/ca/uhn/fhir/rest/server/exceptions/package-summary.html

              The client can also cause exceptions.
              See http://hapifhir.io/apidocs/ca/uhn/fhir/rest/client/exceptions/package-summary.html

              All of them extend from BaseServerResponseException, but could be handled separately if needed.
            */
            LOG.error("A HAPI FHIR exception occurred!", bsr);

            if (bsr.getStatusCode() != 0) {
                LOG.error("HTTP status code from exception: {}", bsr.getStatusCode());
            } else {
                LOG.error("The exception did not have an HTTP status code");
            }

            if (bsr.getResponseMimeType() != null) {
                LOG.error("Response mime type from the exception: {}", bsr.getResponseMimeType());
            } else {
                LOG.error("The exception did not have a response mime type");
            }

            if (bsr.hasResponseHeaders()) {
                LOG.error("Response headers from the exception:");
                for (Map.Entry<String, List<String>> header : bsr.getResponseHeaders().entrySet()) {
                    for (String headerValue : header.getValue()) {
                        LOG.error("Header: \"{}\"   Value: \"{}\"", header.getKey(), headerValue);
                    }
                }
            } else {
                LOG.error("The exception did not have any response headers");
            }

            if (bsr.getResponseBody() != null) {
                LOG.error("Response body from the exception: {}", bsr.getResponseBody());
            } else {
                LOG.error("The exception did not have a response body");
            }

            if (bsr.getAdditionalMessages() != null && !bsr.getAdditionalMessages().isEmpty()) {
                LOG.error("Additional messages from the exception:");
                bsr.getAdditionalMessages().forEach(LOG::error);
            } else {
                LOG.error("The exception did not have any additional messages");
            }

            if (bsr.getOperationOutcome() != null) {
                OperationOutcome oo = (OperationOutcome)bsr.getOperationOutcome();

                /*
                  For each of the operation outcome's issues, we'll log an error message.
                  We'll use the first of the following that is defined in each issue:
                    OperationOutcome.issue.details.text
                    OperationOutcome.issue.diagnostics
                    A generic error message
                 */
                List<String> messagesFromOperationOutcome = new ArrayList<>();
                for (OperationOutcomeIssueComponent ooic : oo.getIssue()) {
                    String messageForIssue;
                    if (ooic.getDetails().hasText()) {
                        messageForIssue = ooic.getDetails().getText();
                    } else if (ooic.hasDiagnostics()) {
                        messageForIssue = ooic.getDiagnostics();
                    } else {
                        messageForIssue = GENERIC_ERROR_MESSAGE;
                    }
                    messagesFromOperationOutcome.add(messageForIssue);
                }

                if (!messagesFromOperationOutcome.isEmpty()) {
                    LOG.error("Here are the error messages from each of the operation outcome issues:");
                    messagesFromOperationOutcome.forEach(LOG::error);
                }
            } else {
                LOG.error("The exception did not have an operation outcome");
            }
        } catch (Exception e) {
            LOG.error("Something really bad happened!", e);
        }
    }
}
