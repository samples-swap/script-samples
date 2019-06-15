import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import com.google.gson.Gson;

List<String> sendRequest(String url, String method, Map<String,Object> body) {


        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(3000)
                .build();

        StringEntity entity = new StringEntity(new Gson().toJson(body), "UTF-8");


        HttpUriRequest request = RequestBuilder.create(method)
                .setConfig(requestConfig)
                .setUri(url)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                .setEntity(entity)
                .build();
                
  String req = "REQUEST:" + "\n" + request.getRequestLine() + "\n" + "Headers: " +
                request.getAllHeaders() + "\n" + EntityUtils.toString(entity) + "\n";
                

        HttpClientBuilder.create().build().withCloseable {httpClient ->

            httpClient.execute(request).withCloseable {response ->

                String res = "RESPONSE:" + "\n" + response.getStatusLine() + "\n" + "Headers: " +
                        response.getAllHeaders() + "\n" +
                        (response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "") + "\n";

                System.out.println(req + "\n"  + res );
                
                return Arrays.asList(req, res);
            }
        }
    }

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("Param_1", "Value_1");
        map.put("Param_2", Arrays.asList(1,2,3,4));
        map.put("Param_3", Arrays.asList("a","b","c"));

List test1 = sendRequest("https://hooks.zapier.com/hooks/catch/3320164/az95by","POST", map);
log.info(Arrays.toString(test1));