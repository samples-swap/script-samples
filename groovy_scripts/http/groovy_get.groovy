mport org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;



List<String> sendGet(String url, Map<String,String> body) {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(3000)
                .build();

        RequestBuilder requestBuilder = RequestBuilder.get()
                .setConfig(requestConfig)
                .setUri(url)
                .setHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        body.forEach({key, value -> requestBuilder.addParameter(key, value)});
        
        HttpUriRequest request = requestBuilder.build();

        String req = "REQUEST:" + "\n" + request.getRequestLine() + "\n" + "Headers: " +
                request.getAllHeaders() + "\n";

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
        Map<String,String> map = new LinkedHashMap<>();
        map.put("Param_1", "Value_1");
        map.put("Param_2", "Value_2");
        map.put("Param_3", "Value_3");

List test1 = sendGet("https://hooks.zapier.com/hooks/catch/3320164/az95by", map);
log.info(Arrays.toString(test1));
