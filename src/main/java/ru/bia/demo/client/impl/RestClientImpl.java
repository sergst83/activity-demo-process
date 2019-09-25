package ru.bia.demo.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import ru.bia.demo.client.RESTServiceException;
import ru.bia.demo.client.RestClient;
import ru.bia.process.model.Order;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

@Service
public class RestClientImpl implements RestClient {

    private static String defaultUrl = "http://localhost:8082/services/operations";

    @Override
    public Map<String, Object> createPickUpOperation(Map<String, Object> params) throws IOException {
        String url = getUrl(params);
        HttpPost post = new HttpPost(url);
        Order order = getOrder(params);
        String jsonBody = createRequestBody(order);
        HttpEntity httpEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        post.setEntity(httpEntity);
        return performRequest(post);
    }

    @Override
    public Map<String, Object> checkPickUpOperation(Map<String, Object> parameters) throws IOException, RESTServiceException {
        String url = getUrl(parameters);
        Order order = getOrder(parameters);
        String id = order.getId();
        HttpGet get = new HttpGet(url + "/" + id);
        return performRequest(get);
    }

    @Override
    public Map<String, Object> createPutWhOperation(Map<String, Object> parameters) throws IOException, RESTServiceException {
        String url = getUrl(parameters);
        Order order = getOrder(parameters);
        String id = order.getId();
        HttpPut httpPut = new HttpPut(url + "/warehouse/" + id);
        return performRequest(httpPut);
    }

    @Override
    public Map<String, Object> checkPutWhOperation(Map<String, Object> parameters) throws IOException, RESTServiceException {
        String url = getUrl(parameters);
        Order order = getOrder(parameters);
        String id = order.getId();
        HttpGet httpGet = new HttpGet(url + "/warehouse/" + id);
        return performRequest(httpGet);
    }

    private String createRequestBody(Object params) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(params);
    }

    private String getUrl(Map<String, Object> parameters) {
        return StringUtils.defaultIfBlank((String) parameters.get("url"), defaultUrl);
    }

    private Map<String, Object> performRequest(HttpUriRequest request) throws IOException {
        HttpClient client = HttpClients.createDefault();

        HttpResponse response = client.execute(request);
        StatusLine statusLine = response.getStatusLine();
        int responseCode = statusLine.getStatusCode();
        HttpEntity respEntity = response.getEntity();
        String responseBody = null;
        String contentType = null;
        if (respEntity != null) {
            responseBody = EntityUtils.toString(respEntity, Charset.defaultCharset());

            if (respEntity.getContentType() != null) {
                contentType = respEntity.getContentType().getValue();
            }
        }
        if (responseCode >= 200 && responseCode < 300) {
            return postProcessResult(responseBody, contentType);
        } else {
            throw new RESTServiceException(responseCode, StringUtils.substring(responseBody, 0, 254), request.getURI().toString());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postProcessResult(String result, String contentType) {
        try {
            if (contentType.toLowerCase().contains("application/json" )) {
                ObjectMapper mapper = new ObjectMapper();

                return mapper.readValue(result, Map.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to transform request to object",
                    e);
        }
        throw new IllegalArgumentException("Unable to find transformer for content type '" + contentType + "' to handle data " + result);
    }

    private Order getOrder(Map<String, Object> params) {
        Object orderObject = params.get("order");
        if (orderObject instanceof Order) {
            return (Order) orderObject;
        } else if (orderObject instanceof JsonNode) {
            return new ObjectMapper().convertValue(orderObject, Order.class);
        } else {
            throw new RuntimeException("Unable to get order from params");
        }
    }

}
